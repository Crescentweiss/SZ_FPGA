package example

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import SZHDParameters._

class SZHDPredictorTest extends AnyFlatSpec with ChiselScalatestTester {
  "SZHDPredictor" should "correctly compute linear interpolation predictions using datareconstruction" in {
    test(new SZHDPredictor) { dut =>
      
      // Helper function to convert Double to scaled SInt
      def toScaledSInt(d: Double): SInt = {
        (d * scale).toInt.S(fixedPointWidth.W)
      }

      val testData = Seq(
        // (input sequence, startindex, midindex, endindex, expected prediction)
        (Seq(1.0, 2.0, 3.0, 4.0, 5.0) ++ Seq.fill(12)(0.0), 0.U, 2.U, 4.U, 3.0),  // Normal interpolation case
        (Seq(1.0, 2.0, 3.0, 4.0, 5.0) ++ Seq.fill(12)(0.0), 0.U, 0.U, 4.U, 0.0),  // mid == start, prediction should be 0
        (Seq(1.0, 2.0, 3.0, 4.0, 5.0) ++ Seq.fill(12)(0.0), 0.U, 4.U, 4.U, 1.0),  // mid == end, prediction should be datareconstruction(0)
        (Seq(-3.0, 0.0, 3.0, 6.0, 9.0) ++ Seq.fill(12)(0.0), 1.U, 2.U, 4.U, 3.0)  // Other interpolation case
      )

      // Iterate through each test case
      for ((inVec, startIdx, midIdx, endIdx, expectedPrediction) <- testData) {
        
        // Set input data, converting each element to scaled SInt
        dut.io.in.zip(inVec).foreach { case (inPort, value) => 
          inPort.poke(toScaledSInt(value))
        }
        
        dut.io.startindex.poke(startIdx)
        dut.io.midindex.poke(midIdx)
        dut.io.endindex.poke(endIdx)

        // Run a clock cycle
        dut.clock.step(1)

        // Check if the prediction output matches the expected scaled value
        dut.io.prediction.expect(toScaledSInt(expectedPrediction))
      }
    }
  }
}
