package example

import chisel3._
import chisel3.util._
import SZHDParameters._

class SZHDPredictor extends Module with SZHDVariables {
    val io = IO(new Bundle {
        val in = Input(Vec(N, SInt(32.W)))
        val startindex = Input(UInt(5.W))
        val midindex = Input(UInt(5.W))
        val endindex = Input(UInt(5.W))
        val prediction = Output(SInt(32.W))
    })

    // 将输入赋值给 datareconstruction
    datareconstruction := io.in

    // 定义线性插值预测函数
    def linearInterpolationPredictor(
        startindex: UInt,
        midindex: UInt,
        endindex: UInt
    ): SInt = {
        val size = (endindex - startindex).asSInt
        val left = (midindex - startindex).asSInt
        val right = (endindex - midindex).asSInt

        val prediction = Wire(SInt(32.W))
        when(midindex === startindex) {
            prediction := 0.S // start, p1 = 0
        }.elsewhen(midindex === endindex) {
            prediction := datareconstruction(0) // n = log2(N), pN = p1
        }.otherwise {
            prediction := (datareconstruction(startindex) * right + datareconstruction(endindex) * left) / size
        }
        prediction
    }

    // 调用线性插值预测函数，获取预测结果
    val dataprediction = linearInterpolationPredictor(io.startindex, io.midindex, io.endindex)

    // 将预测结果输出
    io.prediction := dataprediction
}

object SZHDPredictorMain extends App {
    println("Generating Verilog for SZHDPredictor...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SZHDPredictor, Array("--target-dir", "generated"))
}
