package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDQuantization extends Module with SZHDVariables {
    val io = IO(new Bundle {
        val value = Input(intTypeHD)
        val quantizedValue = Output(intTypeHD)
        val enable = Input(Bool())
        val done = Output(Bool())
    })

    // Initialize output
    io.quantizedValue := 0.S
    io.done := false.B

    // 状态寄存器
    val quantizedReg = RegInit(0.S(intTypeHD.getWidth.W))
    val doneReg = RegInit(false.B)

    when(io.enable) {
        // 量化逻辑：value 为正数 (value + eb)/(2*eb), value 为负数 (value - eb)/(2*eb)
        quantizedReg := Mux(io.value >= 0.S, (io.value + ebHD) / (eb2HD), (io.value - ebHD) / (eb2HD))
        doneReg := true.B
    }.otherwise {
        quantizedReg := quantizedReg
        doneReg := false.B
    }

    // 输出连接
    io.quantizedValue := quantizedReg
    io.done := doneReg
}

object SZHDQuantizationMain extends App {
    println("Generating Verilog for SZHDQuantization...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SZHDQuantization)
}
