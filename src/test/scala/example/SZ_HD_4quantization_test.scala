package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDQuantizationTest extends AnyFlatSpec with ChiselScalatestTester {
    private def toScaledSInt(d: Int): SInt = (d * scaleHD).toInt.asSInt
    
    private def calculateExpected(input: Int): BigInt = {
        val scaledInput = BigInt(input) * scaleHD
        val ebHDInt = ebHD.litValue
        val eb2HDInt = eb2HD.litValue
        if (scaledInput >= 0) (scaledInput + ebHDInt) / eb2HDInt
        else (scaledInput - ebHDInt) / eb2HDInt
    }

    "SZHDQuantization" should "correctly handle enable/done signals and quantize values" in {
        test(new SZHDQuantization) { dut =>
            val testCases = Seq(
                1, 2, -1, 3, -3, 
                Int.MaxValue / (2 * scaleHD),  // 添加边界测试
                Int.MinValue / (2 * scaleHD)
            )

            // 1. 初始状态测试
            dut.io.enable.poke(false.B)
            dut.clock.step(1)
            dut.io.done.expect(false.B)
            dut.io.quantizedValue.expect(0.S)

            // 2. 测试每个用例
            for (input <- testCases) {
                // 禁用状态测试
                dut.io.value.poke(toScaledSInt(input))
                dut.io.enable.poke(false.B)
                dut.clock.step(1)
                dut.io.done.expect(false.B)
                dut.io.quantizedValue.expect(dut.io.quantizedValue.peek())
                
                // 启用状态测试
                dut.io.enable.poke(true.B)
                dut.clock.step(1)
                dut.io.quantizedValue.expect(calculateExpected(input).asSInt)
                dut.io.done.expect(true.B)
            }
        }
    }

    it should "maintain output when disabled" in {
        test(new SZHDQuantization) { dut =>
            val testValue = 1
            
            // 设置初始值并使能
            dut.io.value.poke(toScaledSInt(testValue))
            dut.io.enable.poke(true.B)
            dut.clock.step(1)
            val expectedOutput = calculateExpected(testValue).asSInt
            dut.io.quantizedValue.expect(expectedOutput)
            
            // 禁用后检查状态
            dut.io.enable.poke(false.B)
            dut.clock.step(1)
            // 应该重置为上次的输出
            dut.io.quantizedValue.expect(expectedOutput)
            dut.io.done.expect(false.B)
        }
    }

    it should "handle consecutive operations correctly" in {
        test(new SZHDQuantization) { dut =>
            val inputs = Seq(1, -1, 2, -2)
            
            for (input <- inputs) {
                dut.io.value.poke(toScaledSInt(input))
                dut.io.enable.poke(true.B)
                dut.clock.step(1)
                dut.io.quantizedValue.expect(calculateExpected(input).asSInt)
                dut.io.done.expect(true.B)
            }
        }
    }
}