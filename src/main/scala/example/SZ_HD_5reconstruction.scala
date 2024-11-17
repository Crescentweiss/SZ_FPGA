package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDReconstruction extends Module with SZHDVariables {
    val io = IO(new Bundle {
        val predictedData = Input(intTypeHD)        // SInt(fixedPointWidthHD.W)
        val quantizedDelta = Input(intTypeHD)       // SInt(fixedPointWidthHD.W) 
        val reconstructedData = Output(intTypeHD)    // SInt(fixedPointWidthHD.W)
        val enable = Input(Bool())
        val done = Output(Bool())
    })

    // Initialize output
    io.reconstructedData := 0.S
    io.done := false.B

    // 状态寄存器
    val reconstructedDataReg = RegInit(0.S(fixedPointWidthHD.W))
    val doneReg = RegInit(false.B)

    // 重建计算逻辑 
    when(io.enable) {
        // 扩展位宽进行乘法运算
        val deltaProduct = Wire(SInt((fixedPointWidthHD + 16).W))
        deltaProduct := io.quantizedDelta * eb2HD
        // 扩展predictedData匹配deltaProduct位宽
        val predictedDataExtended = Wire(SInt((fixedPointWidthHD + 16).W))
        predictedDataExtended := io.predictedData.pad(fixedPointWidthHD + 16)
        // 加法运算并处理可能的溢出
        val sumResult = Wire(SInt((fixedPointWidthHD + 16 + 1).W))
        sumResult := predictedDataExtended + deltaProduct
        // 截断到输出位宽
        reconstructedDataReg := sumResult(fixedPointWidthHD - 1, 0).asSInt
        // 输出done信号
        doneReg := true.B
    }.otherwise {
        reconstructedDataReg := reconstructedDataReg
        doneReg := false.B
    }

    io.reconstructedData := reconstructedDataReg
    io.done := doneReg
}

object SZHDReconstruction extends App {
    println("Generating the SZHDReconstruction...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SZHDReconstruction)
}