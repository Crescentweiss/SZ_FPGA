package example

import chisel3._
import chisel3.util._

object SZHDParameters {
  val scaleHD = 1000000 // Scaling factor to approximate float precision, around 10^-6 precision
  val fixedPointWidthHD = 32 // Using 32-bit SInt to approximate float32
  val intTypeHD = SInt(fixedPointWidthHD.W) // Define as 32-bit width
  val uintTypeHD = UInt(fixedPointWidthHD.W) // Define as 32-bit width-unsigned integer
  val NHD = 17 // 2^n + 1, input data size <= N
  val ebHD = (0.05 * scaleHD).toInt.S(fixedPointWidthHD.W) // Scale the error bound as an SInt
  val eb2HD = (0.1 * scaleHD).toInt.S(fixedPointWidthHD.W) // Scale the error bound as an SInt
  val indexWidthHD = UInt(log2Ceil(NHD).W) // Width of the index for the predictor
}

trait SZHDVariables {
  import SZHDParameters._

  val datasetHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val datapredictionHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val datadeltaHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val dataquantizationHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val dataprequantizationHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val flagprequantizationSUPHD: Array[Boolean] = Array.fill(NHD)(false) // just help produce logical digital circuits
  val datareconstructionHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val flagreconstructionHD = RegInit(VecInit(Seq.fill(NHD)(false.B)))
  val datacompressionHD = RegInit(VecInit(Seq.fill(NHD)(0.U(fixedPointWidthHD.W))))
  val lastbitsHD = RegInit(0.U(6.W))
  val datadecompressionHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val datasetrecoverHD = RegInit(VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W))))
  val flagrecoverHD = RegInit(VecInit(Seq.fill(NHD)(false.B))) // Boolean (bit) type
}
