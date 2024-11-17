package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDPredictorTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDPredictor" should "correctly handle enable/done signals and compute predictions" in {
    test(new SZHDPredictor) { dut =>
      
      // Helper function to convert Double to scaled SInt
      def toScaledSInt(d: Double): SInt = {
        (d * scaleHD).toInt.S
      }

      val testData = Seq(
        // (input sequence, startindex, midindex, endindex, expected prediction)
        (Seq(1.0, 2.0, 3.0, 4.0, 5.0) ++ Seq.fill(12)(0.0), 0.U, 2.U, 4.U, 3.0),
        (Seq(1.0, 2.0, 3.0, 4.0, 5.0) ++ Seq.fill(12)(0.0), 0.U, 0.U, 4.U, 0.0),
        (Seq(1.0, 2.0, 3.0, 4.0, 5.0) ++ Seq.fill(12)(0.0), 0.U, 4.U, 4.U, 1.0)
      )

      // 初始状态检查
      dut.io.enable.poke(false.B)
      dut.clock.step(1)
      dut.io.done.expect(false.B)

      // 测试每个用例
      for ((inVec, startIdx, midIdx, endIdx, expectedPrediction) <- testData) {
          // 设置输入数据
          dut.io.in.zip(inVec).foreach { case (inPort, value) => 
            inPort.poke(toScaledSInt(value))
          }
          
          dut.io.startindex.poke(startIdx)
          dut.io.midindex.poke(midIdx)
          dut.io.endindex.poke(endIdx)
          
          // 先禁用确保初始状态
          dut.io.enable.poke(false.B)
          dut.clock.step(1)
          dut.io.done.expect(false.B)
          
          // 激活使能信号并等待一个时钟周期完成计算
          dut.io.enable.poke(true.B)
          dut.clock.step(1)
          dut.io.done.expect(true.B)
          dut.io.prediction.expect(toScaledSInt(expectedPrediction))
          
          // 验证禁用后的行为
          dut.io.enable.poke(false.B)
          dut.clock.step(1)
          dut.io.done.expect(false.B)
        }
      }
    }
}