package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDQuantizationTest extends AnyFlatSpec with ChiselScalatestTester {
    "SZHDQuantization" should "correctly quantize input values in the same clock cycle" in {
            test(new SZHDQuantization) { dut =>
            // Helper function to apply scaling
            def toScaledSInt(d: Int): SInt = (d * scaleHD).S

            // Set up test cases with input and expected output
            val ebHDInt = ebHD.litValue.toInt
            val eb2HDInt = eb2HD.litValue.toInt
            val testCases = Seq(
                (toScaledSInt(1), ((1 * scaleHD + ebHDInt) / eb2HDInt).S),    // value = 1 * scale
                (toScaledSInt(2), ((2 * scaleHD + ebHDInt) / eb2HDInt).S),    // value = 2 * scale
                (toScaledSInt(-1), ((-1 * scaleHD + ebHDInt) / eb2HDInt).S),  // value = -1 * scale
                (toScaledSInt(3), ((3 * scaleHD + ebHDInt) / eb2HDInt).S),    // value = 3 * scale
                (toScaledSInt(-3), ((-3 * scaleHD + ebHDInt) / eb2HDInt).S)   // value = -3 * scale
            )

            // Run all tests in the same clock cycle
            for ((inputValue, expectedOutput) <- testCases) {
                dut.io.value.poke(inputValue)          // Set the input value
                // Check the expected output
                dut.io.quantizedValue.expect(expectedOutput)
            }

            // Only step the clock once at the end to finalize all checks
            dut.clock.step(1)
        }
    }
}
