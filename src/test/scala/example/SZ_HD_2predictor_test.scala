package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SZHDPredictorTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDPredictor" should "correctly compute linear interpolation predictions using datareconstruction" in {
    test(new SZHDPredictor) { dut =>
      val testData = Seq(
        // (input sequence, startindex, midindex, endindex, expected prediction)
        (Seq(1.S, 2.S, 3.S, 4.S, 5.S) ++ Seq.fill(12)(0.S), 0.U, 2.U, 4.U, 3.S),  // Normal case
        (Seq(1.S, 2.S, 3.S, 4.S, 5.S) ++ Seq.fill(12)(0.S), 0.U, 0.U, 4.U, 0.S),  // mid == start
        (Seq(1.S, 2.S, 3.S, 4.S, 5.S) ++ Seq.fill(12)(0.S), 0.U, 4.U, 4.U, 1.S),  // mid == end
        (Seq(-3.S, 0.S, 3.S, 6.S, 9.S) ++ Seq.fill(12)(0.S), 1.U, 2.U, 4.U, 3.S)  // Other cases
      )
      // 遍历每个测试用例
      for ((inVec, startIdx, midIdx, endIdx, expectedPrediction) <- testData) {
        // 设置输入数据
        dut.io.in.zip(inVec).foreach { case (inPort, value) => inPort.poke(value) }
        dut.io.startindex.poke(startIdx)
        dut.io.midindex.poke(midIdx)
        dut.io.endindex.poke(endIdx)
        // 运行一个时钟周期
        dut.clock.step(1)
        // 检查预测输出是否符合预期
        dut.io.prediction.expect(expectedPrediction)
      }
    }
  }
}
