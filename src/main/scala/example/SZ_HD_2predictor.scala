package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDPredictor extends Module with SZHDVariables {
  val io = IO(new Bundle {
    val in = Input(Vec(NHD, intTypeHD)) // Use global intTypeHD for scaled integer input
    val startindex = Input(UInt(5.W))
    val midindex = Input(UInt(5.W))
    val endindex = Input(UInt(5.W))
    val prediction = Output(intTypeHD) // Use global intTypeHD for scaled integer output
  })

  // Define the linear interpolation predictor function to match your logic
  def linearInterpolationPredictor(
      startindex: UInt,
      midindex: UInt,
      endindex: UInt
  ): SInt = {
    val size = (endindex - startindex).asSInt
    val left = (midindex - startindex).asSInt
    val right = (endindex - midindex).asSInt
    val prediction = Wire(intTypeHD)

    when(midindex === startindex) {
      prediction := 0.S // If at start, prediction is 0
    }.elsewhen(midindex === endindex) {
      prediction := io.in(0) // Use the value at index 0 for the end case
    }.otherwise {
      // Perform the scaled linear interpolation calculation
      prediction := (io.in(startindex) * right / size) + (io.in(endindex) * left / size)
    }
    prediction
  }

  // Call the linear interpolation predictor function and get the prediction result
  val predictedValue = linearInterpolationPredictor(io.startindex, io.midindex, io.endindex)

  // Output the prediction result
  io.prediction := predictedValue
}

object SZHDPredictorMain extends App {
  println("Generating Verilog for SZHDPredictor...")
  (new chisel3.stage.ChiselStage).emitVerilog(new SZHDPredictor, Array("--target-dir", "generated"))
}