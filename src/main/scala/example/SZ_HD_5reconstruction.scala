package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDReconstruction extends Module with SZHDVariables {
    val io = IO(new Bundle {
        val predictedData     = Input(intTypeHD)        // SInt(fixedPointWidthHD.W)
        val quantizedDelta    = Input(SInt(16.W))       // 16-bit quantized delta
        val reconstructedData = Output(intTypeHD)       // SInt(fixedPointWidthHD.W)
        val flagIndex         = Input(UInt(log2Ceil(NHD).W)) // Index for the flag vector
        val setFlagTrue       = Input(Bool())           // Signal to set a flag to true
        val setAllFlagsFalse  = Input(Bool())           // Signal to reset all flags to false
        val flagOut           = Output(Vec(NHD, Bool()))
    })

    // Compute deltaProduct = quantizedDelta * eb2HD
    val deltaProduct = Wire(SInt((fixedPointWidthHD + 16).W))
    deltaProduct := io.quantizedDelta * eb2HD

    // Sign-extend predictedData to match deltaProduct width
    val predictedDataExtended = Wire(SInt((fixedPointWidthHD + 16).W))
    predictedDataExtended := io.predictedData.pad(fixedPointWidthHD + 16)

    // Compute reconstructedData = predictedData + deltaProduct
    val sumResult = Wire(SInt((fixedPointWidthHD + 16 + 1).W)) // Extra bit for overflow
    sumResult := predictedDataExtended + deltaProduct

    // Assign the reconstructed data, truncating or extending to fixedPointWidthHD bits
    io.reconstructedData := sumResult(fixedPointWidthHD - 1, 0).asSInt

    // Flag manipulation logic
    when (io.setFlagTrue) {
        flagreconstructionHD(io.flagIndex) := true.B
    }

    when (io.setAllFlagsFalse) {
        // Reset all flags to false
        for (i <- 0 until NHD) {
            flagreconstructionHD(i) := false.B
        }
    }

    // Connect internal flags to output
    io.flagOut := flagreconstructionHD
}

object SZHDReconstructionMain extends App {
    println("Generating Verilog for SZHDReconstruction...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SZHDReconstruction)
}
