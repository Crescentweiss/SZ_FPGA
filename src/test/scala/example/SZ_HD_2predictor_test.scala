package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SZHDPredictorTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDPredictor" should "correctly compute linear interpolation predictions using datareconstruction" in {
    test(new SZHDPredictor) { dut =>
      val testData = Seq(
        // (输入向量, 起始索引, 中间索引, 结束索引, 期望输出)
        (Seq(1.S, 2.S, 3.S, 4.S, 5.S), 0.U, 2.U, 4.U, 3.S),  // 正常情况，mid 处于 start 和 end 中间
        (Seq(1.S, 2.S, 3.S, 4.S, 5.S), 0.U, 0.U, 4.U, 0.S),  // mid == start，应该预测为 0
        (Seq(1.S, 2.S, 3.S, 4.S, 5.S), 0.U, 4.U, 4.U, 1.S),  // mid == end，应该预测为 p1
        (Seq(-3.S, 0.S, 3.S, 6.S, 9.S), 1.U, 2.U, 4.U, 3.S) // 其他情况
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
