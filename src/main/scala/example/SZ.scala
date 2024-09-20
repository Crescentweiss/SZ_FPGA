package example

import scala.collection.mutable
import scala.collection.mutable.PriorityQueue
import scala.util.control.Breaks._

object SZData {
  // Define a dataset of integers with initial values
    var dataset: Array[Double] = Array(
        1.53, 2.34, 3.65,
        4.17, 5.92, 6.78,
        7.89, 8.90, 9.01
    )
    val eb_2 = 0.2  // 2*eb, eb = 0.1
    val N = 9  // 2^n + 1, every cycle, input data size <= N
    var dataprediction: Array[Double] = Array.fill(dataset.length)(0.0)
    var datadelta: Array[Double] = Array.fill(dataset.length)(0.0)
    var dataquantization: Array[Int] = Array.fill(dataset.length)(0)
    var datareconstruction: Array[Double] = Array.fill(dataset.length)(0.0)
    var flagreconstruction: Array[Boolean] = Array.fill(dataset.length)(false) // true: reconstructed, false: not reconstructed
    var encodedictionary: mutable.Map[Int, String] = mutable.Map()
    var datacompression: Int = -1  // -1 means empty
}

object SZprediction extends App {
    import SZData._

    def LinearInterpolationPredictor(reconstructeddata: Array[Double], startindex: Int, midindex: Int, endindex: Int): Double = {
        val size = endindex - startindex
        val left = midindex - startindex
        val right = endindex - midindex
        val datalength = reconstructeddata.length

        if (midindex == startindex) {
            dataprediction(midindex) = 0.0  // start, p1 = 0
            return dataprediction(midindex)
        } else if (midindex == endindex) {
            dataprediction(midindex) = reconstructeddata(0) // n = log2(N), pN = p1
            return dataprediction(midindex)
        } else {
            dataprediction(midindex) = reconstructeddata(startindex) * (right) / size + reconstructeddata(endindex) * (left) / size
            return dataprediction(midindex)
        }
    }
}

object SZdelta extends App {
    import SZData._
    // calculate the difference between this cycle predicted value and the last cycle predicted value
    def delta(actual: Double, predicted: Double): Double = {
        return (actual - predicted)
    }
}

object SZquantization extends App {
    import SZData._
    // quantize the predicted value
    def quantize(value: Double): Int = {
        return Math.round(value/SZData.eb_2).toInt 
    }
}

object SZreconstruction extends App {
    import SZData._
    // restore data by last cycle predicted value + reconstructed quantized delta
    def reconstructData(predictedData: Double, quantizedDelta: Int): Double = {
        val reconstructeddata = predictedData + quantizedDelta*eb_2
        return reconstructeddata
    }

    def Truereconstruction(index: Int): Unit = {
        flagreconstruction(index) = true  // reconstructed
    }

    def Falsereconstruction(): Unit = {
        for (index <- dataset.indices) {
            flagreconstruction(index) = false  // new reconstruction step, not reconstructed
        }
    }
}

object SZpipeline extends App {
    import SZData._
    import SZprediction._
    import SZdelta._
    import SZquantization._
    import SZreconstruction._
    
    // restore data by last cycle predicted value + reconstructed quantized delta
    def quantizationpipeline(): Array[Int] = {
        val Ncurrent = dataset.length

        if (Ncurrent == N) {
            val n = Math.ceil(Math.log(N) / Math.log(2)).toInt + 1
            for (i <- n to 0 by -1) {
                for (index <- 0 to N by Math.pow(2, i).toInt) {
                    val startindex = index
                    val midindex = if (index + Math.pow(2, i-1).toInt > N - 1) 0 else index + Math.pow(2, i-1).toInt
                    val endindex = Math.min(index + Math.pow(2, i).toInt, N - 1)
                    breakable {
                        if (midindex < 0 || midindex >= N || flagreconstruction(midindex)) {
                            // to the next cycle
                            break()
                        }
                        dataprediction(midindex) = LinearInterpolationPredictor(datareconstruction, startindex, midindex, endindex)
                        datadelta(midindex) = delta(dataset(midindex), dataprediction(midindex))
                        dataquantization(midindex) = quantize(datadelta(midindex))
                        datareconstruction(midindex) = reconstructData(dataprediction(midindex), dataquantization(midindex))
                        Truereconstruction(midindex)
                    }  
                }
            }
        } else {
            val n = Math.ceil(Math.log(Ncurrent) / Math.log(2)).toInt + 1
            for (i <- n to 0 by -1) {
                for (index <- 0 to Ncurrent by Math.pow(2, i).toInt) {
                    val startindex = index
                    val midindex = if (index + Math.pow(2, i-1).toInt > Ncurrent - 1) 0 else index + Math.pow(2, i-1).toInt
                    val endindex = Math.min(index + Math.pow(2, i).toInt, Ncurrent - 1)
                    breakable {
                        if (midindex < 0 || midindex >= Ncurrent || flagreconstruction(midindex)) {
                            // to the next cycle
                            break()
                        }
                        dataprediction(midindex) = LinearInterpolationPredictor(datareconstruction, startindex, midindex, endindex)
                        datadelta(midindex) = delta(dataset(midindex), dataprediction(midindex))
                        dataquantization(midindex) = quantize(datadelta(midindex))
                        datareconstruction(midindex) = reconstructData(dataprediction(midindex), dataquantization(midindex))
                        Truereconstruction(midindex)
                    }   
                }
            }
        }
        Falsereconstruction() // new quantization step
        dataquantization
    }
}

object SZcompression extends App {
    import SZData._
    // compress the quantized values by Huffman encoding
    case class Node(value: Int, frequency: Int, left: Option[Node] = None, right: Option[Node] = None)

    def buildHuffmanTree(dataquantization: Array[Int]): Node = {
        if (dataquantization.isEmpty) return Node(-1, 0, None, None)
        // count the frequency of each value
        val frequencyMap: mutable.Map[Int, Int] = mutable.Map()
        dataquantization.foreach(value => {
            if (frequencyMap.contains(value)) {
                frequencyMap(value) += 1
            } else {
                frequencyMap(value) = 1
            }
        })

        // order the values by frequency from low to high
        val priorityQueue: PriorityQueue[Node] = PriorityQueue()(Ordering.by(node => (-node.frequency, -node.value))) //because max heap, so minus the frequency, low to high     
        val sortedFrequencyMap = frequencyMap.toSeq.sortBy(_._2)
        sortedFrequencyMap.foreach { case (value, frequency) =>
            priorityQueue.enqueue(Node(value, frequency, None, None))
        }

        // build the huffman tree, left is low, right is high
        while (priorityQueue.size > 1) {
            val left = priorityQueue.dequeue()
            val right = priorityQueue.dequeue()         
            var parent: Node = Node(left.value + right.value , left.frequency + right.frequency, Some(left), Some(right)) 
            priorityQueue.enqueue(parent)
        }

        // return the root of the huffman tree if the priority queue is not empty
        priorityQueue.dequeue()
    }

    // print the huffman tree
    def printTree(root: Node): Unit = {
        if (root.value == -1 && root.frequency == 0) {
            println("Empty tree")
            return
        }
        val queue = mutable.Queue[(Node, String)]()
        queue.enqueue((root, ""))
        while (queue.nonEmpty) {
            val levelSize = queue.size
            val levelNodes = mutable.ArrayBuffer[String]()
            for (_ <- 0 until levelSize) {
                val (node, prefix) = queue.dequeue()
                levelNodes += prefix + s"(${node.value}, ${node.frequency})"

                node.left.foreach(leftNode => queue.enqueue((leftNode, prefix + "  ")))
                node.right.foreach(rightNode => queue.enqueue((rightNode, prefix + "  ")))
            }
            println(levelNodes.mkString(" "))
        }
    }

    // generate huffman encoding dictionary for each value
    def generateHuffmanCodes(huffmanTree: Node): mutable.Map[Int, String] = {
        if (huffmanTree.value == -1) return mutable.Map()

        val huffmanCodes: mutable.Map[Int, String] = mutable.Map()

        def generateCodes(node: Node, prefix: String): Unit = {
            if (node.left.isEmpty && node.right.isEmpty) {
                huffmanCodes(node.value) = prefix
            } else {
                node.left.foreach(leftNode => generateCodes(leftNode, prefix + "0"))
                node.right.foreach(rightNode => generateCodes(rightNode, prefix + "1"))
            }
        }

        //return the huffman dictionary
        if (huffmanTree.left.isEmpty && huffmanTree.right.isEmpty) {
            huffmanCodes(huffmanTree.value) = "1"
            huffmanCodes
        } else {
            generateCodes(huffmanTree, "")
        }
        huffmanCodes
    }

    // print the huffman encoding dictionary
    def printHuffmanCodes(encodedictionary: mutable.Map[Int, String]): Unit = {
        if (encodedictionary.isEmpty) {
            println("Empty dictionary")
            return
        }

        println("Huffman encoding dictionary:")
        encodedictionary.foreach { case (value, code) =>
            println(s"$value: $code")
        }
    }

    // compress the quantized values by huffman encoding
    def HuffmanEncoding(quantizedDelta: Array[Int], encodedictionary: mutable.Map[Int, String]): Int = {
        if (quantizedDelta.isEmpty) return -1
        // 根据字典压缩数据
        val compressedData: mutable.StringBuilder = new mutable.StringBuilder()
        quantizedDelta.foreach(value => {
            val code = encodedictionary(value)
            compressedData.append(code)
        })
        // turn the binary string to integer
        Integer.parseInt(compressedData.toString(), 2)
    }
}

object SZdecompression extends App {

}

object main extends App {
    import SZData._
    import SZprediction._
    import SZdelta._
    import SZquantization._
    import SZreconstruction._
    import SZpipeline._
    import SZcompression._

}