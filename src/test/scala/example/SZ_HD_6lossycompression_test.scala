package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._
import scala.util.Random

class SZHDLossycompressionTest extends AnyFlatSpec with ChiselScalatestTester {
    val rng = new Random(42)
    
    // 状态名称映射
    val stateNames = Map(
        0 -> "Idle",
        1 -> "PreQuantize", 
        2 -> "Predict",
        3 -> "DeltaCalc",
        4 -> "Done"
    )

    def printState(dut: SZHDLossycompression, cycle: Int = -1): Unit = {
        val state = stateNames(dut.io.state.peek().litValue.toInt)
        if(cycle >= 0) {
        println(s"Cycle $cycle: State = $state")
        } else {
        println(s"Current State: $state")
        }
    }

    def printDebugInfo(dut: SZHDLossycompression, datasetHD_test: Seq[Int]): Unit = {
        println("\n=== Debug Information ===")
        // 量化阶段
        println("Quantization Stage:")
        datasetHD_test.zipWithIndex.foreach { case (value, i) =>
            println(f"  [$i%2d] Input: $value%10d, Quantized: ${dut.io.debug_quantized(i).peek().litValue}%d")
        }
        
        // 预测阶段
        println("\nPrediction Stage:")
        for (i <- 0 until NHD) {
            println(f"  [$i%2d] Input: ${dut.io.predictor_inputs(i).peek().litValue}%10d")
            println(f"       Predicted: ${dut.io.debug_predicted(i).peek().litValue}%10d")
        }
        
        // 差值计算阶段
        println("\nDelta Calculation Stage:")
        for (i <- 0 until NHD) {
            println(f"  [$i%2d] Actual: ${dut.io.delta_inputs(i).peek().litValue}%10d")
            println(f"       Delta: ${dut.io.debug_delta(i).peek().litValue}%10d")
        }
        println("=====================\n")
    }
    
    it should "process data correctly at all test points" in {
        test(new SZHDLossycompression) { dut =>
            // 1. 生成和设置输入数据
            val datasetHD_test = Seq.fill(NHD)((rng.nextDouble() * 20 - 10) * scaleHD).map(_.toInt)
            val quantizedData_test = Array.fill(NHD)(0)
            
            datasetHD_test.zipWithIndex.foreach { case (value, i) =>
                dut.io.inputData(i).poke(value.S)
            }
            
            // 2. 状态机控制和监控
            dut.io.enable.poke(true.B)
            printState(dut)
            
            var cycles = 0
            while (!dut.io.done.peek().litToBoolean && cycles < 10) {
                dut.clock.step(1)
                cycles += 1
                printState(dut, cycles)
                printDebugInfo(dut, datasetHD_test)
            }

            // 3. 计算期望值
            for (i <- 0 until NHD) {
                val value = datasetHD_test(i).toDouble
                val ebValue = ebHD.litValue.toDouble
                val eb2Value = eb2HD.litValue.toDouble
                quantizedData_test(i) = if (value >= 0) 
                ((value + ebValue) / eb2Value).toInt
                else 
                ((value - ebValue) / eb2Value).toInt
            }

            // 4. 验证输出
            val expectedOutputs = Map(
                0 -> (quantizedData_test(0) - 0),
                NHD-1 -> (quantizedData_test(NHD-1) - quantizedData_test(0)),
                NHD/2 -> (quantizedData_test(NHD/2) - (quantizedData_test(0) + quantizedData_test(NHD-1)) / 2),
                NHD/4 -> (quantizedData_test(NHD/4) - (quantizedData_test(0) + quantizedData_test(NHD/2)) / 2),
                NHD/8 -> (quantizedData_test(NHD/8) - (quantizedData_test(0) + quantizedData_test(NHD/4)) / 2)
            )

            println("\n=== Verification Results ===")
            expectedOutputs.foreach { case (index, expectedValue) =>
                val actualValue = dut.io.outputData(index).peek().litValue
                println(f"Index $index%2d: Expected = $expectedValue%6d, Actual = $actualValue%6d")
                dut.io.outputData(index).expect(expectedValue.S(fixedPointWidthHD.W))
            }
        }
    }

    it should "maintain correct hierarchy relationships" in {
        test(new SZHDLossycompression) { dut =>
            // 使用普通Scala集合和运算
            val testData = Seq.fill(NHD)(rng.nextInt(100))
            
            // 设置输入
            testData.zipWithIndex.foreach { case (value, i) =>
                dut.io.inputData(i).poke(value.S)
            }
            
            dut.io.enable.poke(true.B)
            
            // 等待完成
            while (!dut.io.done.peek().litToBoolean) {
                dut.clock.step(1)
            }
            
            // 验证输出稳定性
            val firstOutput = dut.io.outputData.peek()
            dut.clock.step(5)
            dut.io.outputData.expect(firstOutput)
        }
    }
}