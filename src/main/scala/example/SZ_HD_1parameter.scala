package example

import chisel3._
import chisel3.util._

object SZHDParameters {
  val scale = 1000000 // Scaling factor to approximate float precision, around 10^-6 precision
  val fixedPointWidth = 32 // Using 32-bit SInt to approximate float32

  // Define as SInt instead of FixedPoint for scaled floating-point representation
  val intType = SInt(fixedPointWidth.W) // Define as 32-bit width
  val N = 17 // 2^n + 1, input data size <= N
  val eb = (1 * scale).toInt.S(fixedPointWidth.W) // Scale the error bound as an SInt
  val eb_2 = (2 * scale).toInt.S(fixedPointWidth.W) // Scale the error bound multiplied by 2
}

trait SZHDVariables {
  import SZHDParameters._

  val dataset = Reg(Vec(N, intType))
  val dataprediction = Reg(Vec(N, intType))
  val datadelta = Reg(Vec(N, intType))
  val dataquantization = Reg(Vec(N, SInt(16.W))) // Keep quantization data as 16-bit SInt
  val datareconstruction = Reg(Vec(N, intType))
  val flagreconstruction = RegInit(VecInit(Seq.fill(N)(false.B))) // Boolean (bit) type
  val datacompression = Reg(Vec(N, UInt(32.W))) // Unsigned integer for compression
  val lastbits = RegInit(0.U(6.W)) // Last few bits of compressed data
  val datadecompression = Reg(Vec(N, SInt(16.W))) // Use SInt for decompression data
  val datasetrecover = Reg(Vec(N, intType))
  val flagrecover = RegInit(VecInit(Seq.fill(N)(false.B))) // Boolean (bit) type
}
