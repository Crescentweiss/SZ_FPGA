package example

import chisel3._
import chisel3.util._
import scala.util.control.Breaks._
import SZHDParameters._
import scala.collection.mutable.ArrayBuffer

class SZHDLossycompression extends Module with SZHDVariables {
    val io = IO(new Bundle {
        val inputData = Input(Vec(NHD, intTypeHD))  
        val outputData = Output(Vec(NHD, intTypeHD)) 
        val enable = Input(Bool())
        val done = Output(Bool())                    

        // 用于测试
        val quantizer_inputs = Output(Vec(NHD, intTypeHD))    // 量化器输入
        val debug_quantized = Output(Vec(NHD, intTypeHD))     // 量化结果
        // 预测器相关
        val predictor_inputs = Output(Vec(NHD, intTypeHD))    // 预测器输入
        val debug_predicted = Output(Vec(NHD, intTypeHD))     // 预测结果
        // 差值计算相关
        val delta_inputs = Output(Vec(NHD, intTypeHD))        // 差值计算输入
        val debug_delta = Output(Vec(NHD, intTypeHD))         // 差值结果
        val state = Output(UInt(3.W))
    })

    // Initialize output
    io.outputData := VecInit(Seq.fill(NHD)(0.S(fixedPointWidthHD.W)))
    io.done := false.B

    // 实例化各模块
    val prequantizers = Array.fill(NHD)(Module(new SZHDQuantization))
    val predictors = Array.fill(NHD)(Module(new SZHDPredictor))
    val deltaCalcs = Array.fill(NHD)(Module(new SZHDDelta))

    // 状态定义
    val sIdle :: sPreQuantize :: sPredict :: sDeltaCalc :: sDone :: Nil = Enum(5)
    val state = RegInit(sIdle)
    val done = RegInit(false.B)

    // 默认禁用所有子模块
    prequantizers.foreach(_.io.enable := false.B)
    predictors.foreach(_.io.enable := false.B)
    deltaCalcs.foreach(_.io.enable := false.B)


    // 连接预量化器
    for (i <- 0 until NHD) {
        prequantizers(i).io.value := io.inputData(i)
        dataprequantizationHD(i) := prequantizers(i).io.quantizedValue
    }

    // 连接预测器
    // generate index mapping
    var indexMapping: Array[Array[Int]] = Array.fill(NHD, 3)(0)
    val level = log2Ceil(NHD) + 1
    for (i <- level to 0 by -1) {  
        for (index <- 0 to NHD by (1 << i) if index < NHD) {
            val startIndex = index
            val midIndex = if (index + (1 << (i-1)) >= NHD) 0  
                        else index + (1 << (i-1))
            val endIndex = if (index + (1 << i) >= NHD) NHD - 1 
                        else index + (1 << i)
            breakable {
                if (midIndex < 0 || midIndex >= NHD || flagprequantizationSUPHD(midIndex)) {
                    // to the next cycle
                    break()
                }
                indexMapping(midIndex) = Array(startIndex, midIndex, endIndex)  // 0: start, 1: mid, 2: end
                flagprequantizationSUPHD(midIndex) = true
            }
        }
    }
    flagprequantizationSUPHD.indices.foreach(i => flagprequantizationSUPHD(i) = false)
    // connect hardware
    for (index <- 0 until NHD) {
        for (j <- 0 until NHD) {
            predictors(index).io.in(j) := prequantizers(j).io.quantizedValue
        }
        predictors(index).io.startindex := indexMapping(index)(0).U(log2Ceil(NHD).W)
        predictors(index).io.midindex := indexMapping(index)(1).U(log2Ceil(NHD).W)
        predictors(index).io.endindex := indexMapping(index)(2).U(log2Ceil(NHD).W)
        datapredictionHD(index) := Mux(io.enable, predictors(index).io.prediction, 0.S(fixedPointWidthHD.W))
    }

    // 连接差值计算
    for (i <- 0 until NHD) {
        deltaCalcs(i).io.actual := prequantizers(i).io.quantizedValue
        deltaCalcs(i).io.predicted := predictors(i).io.prediction
        datadeltaHD(i) := deltaCalcs(i).io.deltaOut
    }

    when(io.enable) {
        // 状态机控制
        switch(state) {
            is(sIdle) {
                when(io.enable) {
                    state := sPreQuantize
                    done := false.B
                }
            }
            
            is(sPreQuantize) {
                prequantizers.foreach(_.io.enable := true.B)
                when(VecInit(prequantizers.map(_.io.done)).asUInt.andR) {
                    state := sPredict
                }
            }
            
            is(sPredict) {
                predictors.foreach(_.io.enable := true.B)
                when(VecInit(predictors.map(_.io.done)).asUInt.andR) {
                    state := sDeltaCalc
                }
            }
            
            is(sDeltaCalc) {
                deltaCalcs.foreach(_.io.enable := true.B)
                when(VecInit(deltaCalcs.map(_.io.done)).asUInt.andR) {
                    state := sDone
                }
            }
            
            is(sDone) {
                done := true.B
                state := sIdle
            }
        }
    }.otherwise {
        // 禁用时重置所有状态
        state := sIdle
        done := false.B
        flagprequantizationSUPHD.indices.foreach(i => flagprequantizationSUPHD(i) = false)
        // 重置为上次cycle的值
        dataprequantizationHD := dataprequantizationHD
        datapredictionHD  := datapredictionHD
        datadeltaHD := datadeltaHD
    }

    io.done := done
    io.outputData := datadeltaHD

    // 连接调试端口
    io.quantizer_inputs := io.inputData
    io.debug_quantized := VecInit(prequantizers.map(_.io.quantizedValue))
    io.predictor_inputs := VecInit(predictors.map(_.io.in(0)))
    io.debug_predicted := VecInit(predictors.map(_.io.prediction))
    io.delta_inputs := VecInit(deltaCalcs.map(_.io.actual))
    io.debug_delta := VecInit(deltaCalcs.map(_.io.deltaOut))
    io.state := state
}

object SZHDLossycompression extends App {
    println("Generating the SZHDLossycompression...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SZHDLossycompression)
}