package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDReconstructionTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDReconstruction" should "correctly reconstruct data and manipulate flags" in {
    test(new SZHDReconstruction) { dut =>
        // 初始化输入信号
        dut.io.predictedData.poke(0.S)
        dut.io.quantizedDelta.poke(0.S)
        dut.io.flagIndex.poke(0.U)
        dut.io.setFlagTrue.poke(false.B)
        dut.io.setAllFlagsFalse.poke(false.B)
        dut.clock.step(1)

        // 提取整数形式的 ebHD 和 eb2HD
        val ebHDInt = (0.1 * scaleHD).toInt
        val eb2HDInt = (0.2 * scaleHD).toInt

        // 定义辅助函数
        def toScaledSInt(value: Double): SInt = {
            (value * scaleHD).toInt.S(fixedPointWidthHD.W)
        }

        // 定义测试用例 (predictedData, quantizedDelta)
        val testCases = Seq(
            (toScaledSInt(1.0), ((1.0 * scaleHD + ebHDInt) / eb2HDInt).toInt.S(fixedPointWidthHD.W)),
            (toScaledSInt(-0.5), ((-0.5 * scaleHD + ebHDInt) / eb2HDInt).toInt.S(fixedPointWidthHD.W)),
            (toScaledSInt(1.234567), ((1.234567 * scaleHD + ebHDInt) / eb2HDInt).toInt.S(fixedPointWidthHD.W))
        )

        for ((predictedData, quantizedDelta) <- testCases) {
            // 应用输入信号
            dut.io.predictedData.poke(predictedData)
            dut.io.quantizedDelta.poke(quantizedDelta)
            dut.clock.step(1)

            // 计算期望的重构数据
            val expectedDataValue = predictedData.litValue + quantizedDelta.litValue * eb2HD.litValue
            val expectedData = expectedDataValue.S(fixedPointWidthHD.W)

            // 检查输出是否正确
            dut.io.reconstructedData.expect(expectedData)

            // 测试标志位操作
            // 设置特定标志位为 true
            dut.io.flagIndex.poke(5.U)
            dut.io.setFlagTrue.poke(true.B)
            dut.clock.step(1)
            dut.io.setFlagTrue.poke(false.B)

            // 检查索引为 5 的标志位是否被设置为 true
            dut.io.flagOut(5).expect(true.B)

            // 重置所有标志位为 false
            dut.io.setAllFlagsFalse.poke(true.B)
            dut.clock.step(1)
            dut.io.setAllFlagsFalse.poke(false.B)

            // 检查所有标志位是否被重置为 false
            for (i <- 0 until NHD) {
                dut.io.flagOut(i).expect(false.B)
            }
        }
    }
  }
}
