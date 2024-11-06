package example

import chisel3._
import chisel3.util._

object SZHDParameters {
  val scaleHD = 1000000 // Scaling factor to approximate float precision, around 10^-6 precision
  val fixedPointWidthHD = 32 // Using 32-bit SInt to approximate float32

  // Define as SInt instead of FixedPoint for scaled floating-point representation
  val intTypeHD = SInt(fixedPointWidthHD.W) // Define as 32-bit width
  val NHD = 17 // 2^n + 1, input data size <= N
  val ebHD = (0.1 * scaleHD).toInt.S(fixedPointWidthHD.W) // Scale the error bound as an SInt
  val eb2HD = (0.2 * scaleHD).toInt.S(fixedPointWidthHD.W) // Scale the error bound as an SInt
}

trait SZHDVariables {
  import SZHDParameters._

  val datasetHD = Reg(Vec(NHD, intTypeHD))
  val datapredictionHD = Reg(Vec(NHD, intTypeHD))
  val datadeltaHD = Reg(Vec(NHD, intTypeHD))
  val dataquantizationHD = Reg(Vec(NHD, SInt(16.W))) // Keep quantization data as 16-bit SInt
  val datareconstructionHD = Reg(Vec(NHD, intTypeHD))
  val flagreconstructionHD = RegInit(VecInit(Seq.fill(NHD)(false.B))) // Boolean (bit) type
  val datacompressionHD = Reg(Vec(NHD, UInt(32.W))) // Unsigned integer for compression
  val lastbitsHD = RegInit(0.U(6.W)) // Last few bits of compressed data
  val datadecompressionHD = Reg(Vec(NHD, SInt(16.W))) // Use SInt for decompression data
  val datasetrecoverHD = Reg(Vec(NHD, intTypeHD))
  val flagrecoverHD = RegInit(VecInit(Seq.fill(NHD)(false.B))) // Boolean (bit) type
}
