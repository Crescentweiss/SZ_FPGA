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

    println(dataset.mkString(" "))
    println(dataprediction.mkString(" "))
    println(datadelta.mkString(" "))
    println(dataquantization.mkString(" "))
    println(datareconstruction.mkString(" "))
    
    val expecedReconstructedData = Array(4, -2, -2, 0, 20, 0, 26, 35)
    assert(result.sameElements(expecedReconstructedData.map(_.toInt)))
  }
}

class SZcompressionTest extends AnyFunSuite {
  test("HuffmanEncoding should compress dataquantization correctly") {
    val dataquantization = Array(1, 2, 2, 3, 3, 3, 4, 4, 4, 4)
    val expectedCompression = Integer.parseInt("1101111111010100000", 2)
    var encodedictionary = mutable.Map[Int, String]()
    val huffmanTree = SZcompression.buildHuffmanTree(dataquantization)
    encodedictionary = SZcompression.generateHuffmanCodes(huffmanTree)
    val result = SZcompression.HuffmanEncoding(dataquantization, encodedictionary)
    assert(result == expectedCompression)
  }

  test("HuffmanEncoding should handle empty array") {
    val dataquantization = Array[Int]()
    val expectedCompression = -1
    var encodedictionary = mutable.Map[Int, String]()
    val huffmanTree = SZcompression.buildHuffmanTree(dataquantization)
    encodedictionary = SZcompression.generateHuffmanCodes(huffmanTree)
    val result = SZcompression.HuffmanEncoding(dataquantization, encodedictionary)
    assert(result == expectedCompression)
  }

  test("HuffmanEncoding should handle single element array") {
    val dataquantization = Array(1)
    val expectedCompression = Integer.parseInt("1", 2) // 0
    var encodedictionary = mutable.Map[Int, String]()
    val huffmanTree = SZcompression.buildHuffmanTree(dataquantization)
    encodedictionary = SZcompression.generateHuffmanCodes(huffmanTree)
    val result = SZcompression.HuffmanEncoding(dataquantization, encodedictionary)
    assert(result == expectedCompression)
  }
}
