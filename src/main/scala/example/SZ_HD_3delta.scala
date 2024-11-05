package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDDelta extends Module with SZHDVariables {
  // Define inputs and outputs for delta calculation
  val io = IO(new Bundle {
    val actual = Input(SInt(32.W))
    val predicted = Input(SInt(32.W))
    val deltaOut = Output(SInt(32.W))
  })

  // Calculate the difference between actual and predicted values
  io.deltaOut := io.actual - io.predicted
}

object SZHDDeltaMain extends App {
  println("Generating Verilog for SZHDDelta...")
  (new chisel3.stage.ChiselStage).emitVerilog(new SZHDDelta)
}
