package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDDelta extends Module with SZHDVariables {
  val io = IO(new Bundle {
    val actual = Input(intTypeHD)
    val predicted = Input(intTypeHD)
    val deltaOut = Output(intTypeHD)
    val enable = Input(Bool())
    val done = Output(Bool())
  })

  // Initialize output
  io.deltaOut := 0.S
  io.done := false.B

  // 状态寄存器
  val deltaReg = RegInit(0.S(intTypeHD.getWidth.W))
  val doneReg = RegInit(false.B)
  
  when(io.enable) {
    deltaReg := io.actual - io.predicted
    doneReg := true.B
  }.otherwise {
    deltaReg := deltaReg
    doneReg := false.B
  }

  io.deltaOut := deltaReg
  io.done := doneReg
}

object SZHDDeltaMain extends App {
  println("Generating Verilog for SZHDDelta...")
  (new chisel3.stage.ChiselStage).emitVerilog(new SZHDDelta)
}