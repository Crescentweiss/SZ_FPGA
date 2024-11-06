package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDQuantization extends Module with SZHDVariables {
    val io = IO(new Bundle {
        val value = Input(SInt(fixedPointWidthHD.W))
        val quantizedValue = Output(SInt(16.W)) // Assume 16-bit output based on earlier code
    })
    // Add `eb` for rounding and divide by `2 * eb` for quantization
    io.quantizedValue := ((io.value + ebHD) / eb2HD).asSInt
}

object SZHDQuantizationMain extends App {
    println("Generating Verilog for SZHDQuantization...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SZHDQuantization)
}
