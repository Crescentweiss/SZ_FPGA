package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDReconstructionTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDReconstruction" should "correctly reconstruct data with enable control" in {
    test(new SZHDReconstruction) { dut =>
        // 初始化输入信号
        dut.io.predictedData.poke(0.S)
        dut.io.quantizedDelta.poke(0.S)
        dut.io.enable.poke(false.B)
        dut.clock.step(1)

        // 提取整数形式的 eb2HD
        val eb2HDInt = (0.2 * scaleHD).toInt

        // 定义辅助函数
        def toScaledSInt(value: Double): SInt = {
            (value * scaleHD).toInt.S(fixedPointWidthHD.W)
        }

        // 定义测试用例 (predictedData, quantizedDelta)
        val testCases = Seq(
            (toScaledSInt(1.0), ((1.0 * scaleHD) / eb2HDInt).toInt.S(fixedPointWidthHD.W)),
            (toScaledSInt(-0.5), ((-0.5 * scaleHD) / eb2HDInt).toInt.S(fixedPointWidthHD.W)),
            (toScaledSInt(1.234567), ((1.234567 * scaleHD) / eb2HDInt).toInt.S(fixedPointWidthHD.W))
        )

        for ((predictedData, quantizedDelta) <- testCases) {
            // 测试禁用状态
            dut.io.enable.poke(false.B)
            dut.io.predictedData.poke(predictedData)
            dut.io.quantizedDelta.poke(quantizedDelta)
            dut.clock.step(1)
            dut.io.reconstructedData.expect(dut.io.reconstructedData.peek())
            dut.io.done.expect(false.B)

            // 测试使能状态
            dut.io.enable.poke(true.B)
            dut.io.predictedData.poke(predictedData)
            dut.io.quantizedDelta.poke(quantizedDelta)
            dut.clock.step(1)

            // 计算期望的重构数据
            val expectedDataValue = predictedData.litValue + quantizedDelta.litValue * eb2HD.litValue
            val expectedData = expectedDataValue.S(fixedPointWidthHD.W)

            // 检查输出是否正确
            dut.io.reconstructedData.expect(expectedData)
            dut.io.done.expect(true.B)
        }
    }
  }
}