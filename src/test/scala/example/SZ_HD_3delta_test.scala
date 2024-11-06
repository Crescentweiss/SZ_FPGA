package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDDeltaTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDDelta" should "calculate the correct delta" in {
    test(new SZHDDelta) { dut =>
      // Helper function to apply scaling
      def toScaledSInt(d: Int): SInt = (d * scaleHD).S

      // Test case 1: actual = 15, predicted = 10, expected delta = 5
      dut.io.actual.poke(toScaledSInt(15))
      dut.io.predicted.poke(toScaledSInt(10))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(5))

      // Test case 2: actual = 20, predicted = 25, expected delta = -5
      dut.io.actual.poke(toScaledSInt(20))
      dut.io.predicted.poke(toScaledSInt(25))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(-5))

      // Test case 3: actual = -5, predicted = -10, expected delta = 5
      dut.io.actual.poke(toScaledSInt(-5))
      dut.io.predicted.poke(toScaledSInt(-10))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(5))

      // Test case 4: actual = -10, predicted = 10, expected delta = -20
      dut.io.actual.poke(toScaledSInt(-10))
      dut.io.predicted.poke(toScaledSInt(10))
      dut.clock.step(1)
      dut.io.deltaOut.expect(toScaledSInt(-20))
    }
  }
}
