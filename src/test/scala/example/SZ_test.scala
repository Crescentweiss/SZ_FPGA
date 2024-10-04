package example

import munit.FunSuite
import org.scalatest.funsuite.AnyFunSuite
import scala.collection.mutable
import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror

class SZprediction_test extends FunSuite {
  test("LinearInterpolationPredictor should predict correctly") {
    import SZprediction._

    val reconstructData: Array[Double] = Array(0.0, 1.1, 2.2, 3.3, 4.4)
    val startindex: Int = 0
    val midindex: Int = 2
    val endindex: Int = 4

    val predictedData = LinearInterpolationPredictor(reconstructData, startindex, midindex, endindex)
    val expectedPredicteddata = 2.2

    assert(predictedData == expectedPredicteddata)
  }
}

class SZdelta_test extends FunSuite {  
  test("delta should calculate correctly") {
    import SZdelta._
  
    val predicted: Double = 0.0
    val actual: Double = 1.1

    val datadelta: Double = delta(actual, predicted)
    val expectedDeltas: Double = 1.1
    assert(datadelta == expectedDeltas)
  }
}

class SZquantization_test extends FunSuite {
  test("quantize should calculate correctly") {
    import SZData._
    import SZquantization._

    val dataprediction: Double = 0.2
    val expectedQuantizations: Int = 1

    assert(quantize(dataprediction) == expectedQuantizations)
  }
}

class SZpipelineTest extends AnyFunSuite {
  test("quantizationpipeline should return correct quantized data") {
    import SZData._
    import SZprediction._
    import SZdelta._
    import SZquantization._
    import SZreconstruction._
    import SZpipeline._

    dataset = Array(0.8, 1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7)
    dataprediction = Array.fill(dataset.length)(0.0)
    datadelta = Array.fill(dataset.length)(0.0)
    dataquantization = Array.fill(dataset.length)(0)
    datareconstruction = Array.fill(dataset.length)(0.0)
    flagreconstruction = Array.fill(dataset.length)(false) // true: reconstructed, false: not reconstructed

    val result: Array[Int] = SZpipeline.quantizationpipeline()

    // println(dataset.mkString(" "))
    // println(dataprediction.mkString(" "))
    // println(datadelta.mkString(" "))
    // println(dataquantization.mkString(" "))
    // println(datareconstruction.mkString(" "))
    val expecedReconstructedData = Array(4, -2, -2, 0, 20, 0, 26, 35)
    assert(result.sameElements(expecedReconstructedData.map(_.toInt)))
  }
}

class SZcompressionTest extends AnyFunSuite {
  test("HuffmanEncoding should compress dataquantization correctly") {
    val dataquantization = Array(1, 2, 2, 3, 3, 3, 4, 4, 4, 4)
    val expectedCompression = Array(java.lang.Long.parseUnsignedLong("1101111111010100000", 2))
    var encodedictionary = mutable.Map[Int, String]()
    val huffmanTree = SZcompression.buildHuffmanTree(dataquantization)
    encodedictionary = SZcompression.generateHuffmanCodes(huffmanTree)
    val result = SZcompression.HuffmanEncoding(dataquantization, encodedictionary)
    // SZcompression.printHuffmanCodes(encodedictionary)
    // println(result.map(_.toBinaryString).mkString(" "))
    assert(result.sameElements(expectedCompression))
  }

  test("HuffmanEncoding should handle empty array") {
    val dataquantization = Array[Int]()
    val expectedCompression = Array[Long]()
    var encodedictionary = mutable.Map[Int, String]()
    val huffmanTree = SZcompression.buildHuffmanTree(dataquantization)
    encodedictionary = SZcompression.generateHuffmanCodes(huffmanTree)
    val result = SZcompression.HuffmanEncoding(dataquantization, encodedictionary)
    assert(result.sameElements(expectedCompression))
  }

  test("HuffmanEncoding should handle single element array") {
    val dataquantization = Array(1)
    val expectedCompression = Array(java.lang.Long.parseUnsignedLong("1", 2))
    var encodedictionary = mutable.Map[Int, String]()
    val huffmanTree = SZcompression.buildHuffmanTree(dataquantization)
    encodedictionary = SZcompression.generateHuffmanCodes(huffmanTree)
    val result = SZcompression.HuffmanEncoding(dataquantization, encodedictionary)
    assert(result.sameElements(expectedCompression))
  }
}

class SZdecompressionTest extends AnyFunSuite {    
  test("decodeHuffman should decompress compressed data correctly") {
    import SZData._
    import SZdecompression._

    // Setup the Huffman code and compressed data
    val huffmanCodes = mutable.Map(
      1 -> "0",
      2 -> "10",
      3 -> "110",
      4 -> "111"
    )

    // Simulate compressed data (binary string: 0110111110 -> Long array)
    val compressedData = Array(java.lang.Long.parseUnsignedLong("110111110010010", 2))
    val lastbits = 15 // Set lastbits correctly based on the compressed binary string

    // Expected decompressed output
    val expectedDecompressed = Array(3, 4, 3, 1, 2, 1, 2)

    // Decompress the compressed data using SZdecompression
    SZdecompression.decodeHuffman(huffmanCodes, compressedData, lastbits)

    // Debugging: print the decoded values
    println(s"Decoded values: ${SZData.datadecompression.mkString(", ")}")

    // Validate that the decoded values match the expected values
    assert(SZData.datadecompression.sameElements(expectedDecompressed))
  }


  test("repipeline should reconstruct dataset correctly") {
    import SZData._
    import SZprediction._
    import SZreconstruction._
    import SZdecompression._

    datadecompression = Array(4, -2, -2, 0, 20, 0, 26, 35)
    dataprediction = Array.fill(datadecompression.length)(0.0)
    datasetrecover = Array.fill(SZData.datadecompression.length)(0.0)
    flagrecover = Array.fill(SZData.datadecompression.length)(false)

    // control the error bound
    val eb = SZData.eb_2
    val result: Array[Double] = SZdecompression.repipeline()

    // println(datadecompression.mkString(" "))
    // println(dataprediction.mkString(" "))
    // println(datasetrecover.mkString(" "))

    val expectedRecoveryData = Array(0.8, 1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7)
    assert(result.zip(expectedRecoveryData).forall { case (res, exp) => Math.abs(res - exp) < eb }, 
      "One or more elements differ by more than the error bound"
    )
  }
}

