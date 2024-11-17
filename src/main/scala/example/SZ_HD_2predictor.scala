package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDPredictor extends Module with SZHDVariables {
  val io = IO(new Bundle {
    val in = Input(Vec(NHD, intTypeHD))
    val startindex = Input(indexWidthHD)
    val midindex = Input(indexWidthHD)
    val endindex = Input(indexWidthHD)
    val prediction = Output(intTypeHD)
    val enable = Input(Bool())
    val done = Output(Bool())
  })

  // Initialize output
  io.prediction := 0.S
  io.done := false.B

  // 状态寄存器
  val predictionReg = RegInit(0.S(intTypeHD.getWidth.W))
  val doneReg = RegInit(false.B)

  // 预测逻辑
  when(io.enable) {
    when(io.midindex === io.startindex) {
      predictionReg := 0.S
    }.elsewhen(io.midindex === io.endindex) {
      predictionReg := io.in(0)  
    }.otherwise {
      predictionReg := (io.in(io.startindex) + io.in(io.endindex)) >> 1
    }
    doneReg := true.B
  }.otherwise {
    predictionReg := predictionReg
    doneReg := false.B
  }

  // 输出连接
  io.prediction := predictionReg
  io.done := doneReg
}

object SZHDPredictorMain extends App {
  println("Generating Verilog for SZHDPredictor...")
  (new chisel3.stage.ChiselStage).emitVerilog(new SZHDPredictor, Array("--target-dir", "generated"))
}