package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDDeltaTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDDelta" should "calculate the correct delta when enabled" in {
    test(new SZHDDelta) { dut =>
      def toScaledSInt(d: Int): SInt = (d * scaleHD).S
      
      // 初始状态检查
      dut.io.enable.poke(false.B)
      dut.io.done.expect(false.B)
      dut.io.deltaOut.expect(0.S)
      
      // 启用模块并测试计算
      dut.io.enable.poke(true.B)
      
      // Test case 1: actual = 15, predicted = 10, expected = 5
      dut.io.actual.poke(toScaledSInt(15))
      dut.io.predicted.poke(toScaledSInt(10))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(5))
      dut.io.done.expect(true.B)
      
      // Test case 2: actual = 20, predicted = 25, expected = -5
      dut.io.actual.poke(toScaledSInt(20))
      dut.io.predicted.poke(toScaledSInt(25))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(-5))
      dut.io.done.expect(true.B)
      
      // 禁用模块测试
      dut.io.enable.poke(false.B)
      dut.clock.step(1)
      dut.io.done.expect(false.B)
      dut.io.deltaOut.expect(toScaledSInt(-5))
      
      // 重新启用并继续测试
      dut.io.enable.poke(true.B)
      
      // Test case 3: actual = -5, predicted = -10, expected = 5
      dut.io.actual.poke(toScaledSInt(-5))
      dut.io.predicted.poke(toScaledSInt(-10))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(5))
      dut.io.done.expect(true.B)
      
      // Test case 4: actual = -10, predicted = 10, expected = -20
      dut.io.actual.poke(toScaledSInt(-10))
      dut.io.predicted.poke(toScaledSInt(10))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(-20))
      dut.io.done.expect(true.B)
    }
  }
}