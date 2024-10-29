package example

import scala.collection.mutable
import scala.collection.mutable.PriorityQueue
import scala.util.control.Breaks._
import scala.util.Random

object SZData {
  // Define a dataset of integers with initial values
    val eb = 0.1  // eb = 0.1
    val eb_2 = 2*eb  // 2*eb, eb = 0.1
    val N = 17  // 2^n + 1, every cycle, input data size <= N
    var dataset: Array[Double] = Array.fill(N)(BigDecimal(Random.nextDouble() * 100).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
    var dataprediction: Array[Double] = Array.fill(N)(0.0)
    var datadelta: Array[Double] = Array.fill(N)(0.0)
    var dataquantization: Array[Int] = Array.fill(N)(0)
    var datareconstruction: Array[Double] = Array.fill(N)(0.0)
    var flagreconstruction: Array[Boolean] = Array.fill(N)(false) // true: reconstructed, false: not reconstructed
    var encodedictionary: mutable.Map[Int, String] = mutable.Map()
    var datacompression: Array[Long] = Array()  // -1 means 
    var lastbits: Int = 0 // last Array's bits of compressed data
    var datadecompression: Array[Int] = Array()
    var datasetrecover: Array[Double] = Array.fill(N)(0.0)
    var flagrecover: Array[Boolean] = Array.fill(N)(false) // true: recovered, false: not recovered
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

    def Truereconstruction(flag: Array[Boolean], index: Int): Unit = {
        flag(index) = true  // reconstructed
    }

    def Falsereconstruction(flag: Array[Boolean]): Unit = {
        for (index <- dataset.indices) {
            flag(index) = false  // new reconstruction step, not reconstructed
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
                        Truereconstruction(flagreconstruction, midindex)
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
                        Truereconstruction(flagreconstruction, midindex)
                    }   
                }
            }
        }
        Falsereconstruction(flagreconstruction) // new quantization step
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
    def HuffmanEncoding(quantizedDelta: Array[Int], encodedictionary: mutable.Map[Int, String]): Array[Long] = {
        if (quantizedDelta.isEmpty) return Array.emptyLongArray
        // compress data based on the huffman encoding dictionary
        val longArrayBuffer = mutable.ArrayBuffer[Long]()
        var currentLong: Long = 0L
        var bitsFilled: Int = 0

        quantizedDelta.foreach{ value => 
            val code = encodedictionary(value)
            var codeBits = BigInt(code, 2)
            var codeLength = code.length

            while (codeLength > 0) {
                val bitsToFill = 64 - bitsFilled
                val bitsToTake = math.min(bitsToFill, codeLength)

                // pick up codeBits high bits
                val shiftAmount = codeLength - bitsToTake
                val bits = (codeBits >> shiftAmount).toLong & ((1L << bitsToTake) - 1)

                // put this bits into currentLong
                currentLong = (currentLong << bitsToTake) | bits
                bitsFilled += bitsToTake

                // remove the bits we just took from codeBits
                codeBits = codeBits & ((BigInt(1) << shiftAmount) - 1)
                codeLength -= bitsToTake

                // if currentLong is full, add it to the buffer
                if (bitsFilled == 64) {
                    longArrayBuffer.append(currentLong)
                    currentLong = 0L
                    bitsFilled = 0
                }
            }
        }

        // if there are still bits left, left shift and fill the remaining bits
        if (bitsFilled > 0) {
            longArrayBuffer.append(currentLong)
            lastbits = bitsFilled
        } else {
            lastbits = 64  // equal to long type, 64 bits
        }
        datacompression = longArrayBuffer.toArray
        datacompression
    }
}

object SZdecompression extends App {
    import SZData._
    import SZprediction._
    import SZreconstruction._

    def decodeHuffman(huffmanCodes: mutable.Map[Int, String], datacompression: Array[Long], lastbits: Int): Array[Int] = {
        val reverseHuffmanCodes = huffmanCodes.map(_.swap)  // Reverse the Huffman code map for decoding
        val decodedValues = mutable.ArrayBuffer[Int]()

        // Convert compressed data back into a single bitstream string
        val bitStream = new StringBuilder()
        
        // Only add the significant bits from each Long in datacompression
        datacompression.zipWithIndex.foreach { case (longValue, index) =>
            val binaryString = java.lang.Long.toBinaryString(longValue)
            val fullBinaryString = "0" * (64 - binaryString.length) + binaryString // Ensure 64-bit
            if (index == datacompression.length - 1 && lastbits > 0) {
            // For the last value, only take the number of bits specified by lastbits
            bitStream.append(fullBinaryString.takeRight(lastbits))
            } else {
            // For the other values, take all 64 bits
            bitStream.append(fullBinaryString)
            }
        }
        var buffer = ""
        // Decode the bit stream by matching against the Huffman code dictionary
        bitStream.foreach { bit =>
            buffer += bit  // Append each bit to the buffer
            if (reverseHuffmanCodes.contains(buffer)) {
            // When a matching code is found in the Huffman dictionary
            decodedValues.append(reverseHuffmanCodes(buffer).toInt)
            buffer = ""  // Reset the buffer for the next code
            }
        }
        datadecompression = decodedValues.toArray  // Store the decoded values
        datadecompression
    }

    def repipeline(): Array[Double] = {
        val Ncurrent = datadecompression.length
        if (Ncurrent == N) {
            val n = Math.ceil(Math.log(N) / Math.log(2)).toInt + 1
            for (i <- n to 0 by -1) {
                for (index <- 0 until N by Math.pow(2, i).toInt) {
                    val startindex = index
                    val midindex = if (index + Math.pow(2, i - 1).toInt > N - 1) 0 else index + Math.pow(2, i - 1).toInt
                    val endindex = Math.min(index + Math.pow(2, i).toInt, N - 1)
                    breakable {
                    if (midindex < 0 || midindex >= N || flagrecover(midindex)) {
                        break()
                    }
                    dataprediction(midindex) = LinearInterpolationPredictor(datasetrecover, startindex, midindex, endindex)
                    datasetrecover(midindex) = reconstructData(dataprediction(midindex), datadecompression(midindex))
                    Truereconstruction(flagrecover, midindex)
                    }
                }
            }
        } else {
            val n = Math.ceil(Math.log(Ncurrent) / Math.log(2)).toInt + 1
            for (i <- n to 0 by -1) {
                for (index <- 0 until Ncurrent by Math.pow(2, i).toInt) {
                    val startindex = index
                    val midindex = if (index + Math.pow(2, i - 1).toInt > Ncurrent - 1) 0 else index + Math.pow(2, i - 1).toInt
                    val endindex = Math.min(index + Math.pow(2, i).toInt, Ncurrent - 1)
                    breakable {
                    if (midindex < 0 || midindex >= Ncurrent || flagrecover(midindex)) {
                        break()
                    }
                    dataprediction(midindex) = LinearInterpolationPredictor(datasetrecover, startindex, midindex, endindex)
                    datasetrecover(midindex) = reconstructData(dataprediction(midindex), datadecompression(midindex))
                    Truereconstruction(flagrecover, midindex)
                    }
                }
            }
        }
        Falsereconstruction(flagrecover) // new quantization step
        datasetrecover
    }
}

//output compressedRatio, errorBound

object main extends App {
    import SZData._
    import SZprediction._
    import SZdelta._
    import SZquantization._
    import SZreconstruction._
    import SZpipeline._
    import SZcompression._
    import SZdecompression._

    def Compressionpart(): Unit = {
        println("Starting the SZ pipeline...")
        val quantizedData = quantizationpipeline()
        println(s"Quantized Data: ${quantizedData.mkString(", ")}")

        println("Building Huffman Tree...")
        val huffmanTree = buildHuffmanTree(quantizedData)
        println("Huffman Tree built successfully!")
        // printTree(huffmanTree)

        println("Generating Huffman Codes...")
        val huffmanCodes = generateHuffmanCodes(huffmanTree)
        printHuffmanCodes(huffmanCodes)

        println("Compressing the quantized data...")
        val compressedData = HuffmanEncoding(quantizedData, huffmanCodes)
        val binaryCompressedData = compressedData.map(data => data.toBinaryString)
        println("Compressed Data in binary format:")
        binaryCompressedData.foreach(bin => println(f"$bin%s"))
        println(s"Last bits used in compression: $lastbits")

        val originalSize = dataset.length * 64
        val compressedSize = compressedData.length * 64 + lastbits
        val compressionRatio = originalSize.toDouble / compressedSize.toDouble
        println(f"Compression Ratio: $compressionRatio%.2f")

        println(s"Error Bound (eb): $eb")
    }

    def Decompressionpart(): Unit = {
        println("Starting Huffman Decompression...")
        val huffmanCodes = generateHuffmanCodes(buildHuffmanTree(dataquantization))
        val decompressedData = decodeHuffman(huffmanCodes, datacompression, lastbits)
        println(s"Decompressed Data: ${decompressedData.mkString(", ")}")

        println("Starting the SZ Repipeline for data recovery...")
        val recoveredData = repipeline()
        println("data recovered complete")

        // Calculate the average error between the original dataset and the recovered data
        val errors = dataset.zip(recoveredData).map { case (original, recovered) =>
            math.abs(original - recovered)
        }
        val averageError = errors.sum / errors.length
        println(f"Average reconstruction error: $averageError%.4f")
        val maxError = errors.max
        println(f"Max reconstruction error: $maxError%.4f")
    }

    // Calling the Compression and Decompression functions
    Compressionpart()
    Decompressionpart()
    
    println(s"Original Data: ${dataset.mkString(", ")}")
    println(s"Recovered Data: ${datasetrecover.mkString(", ")}")
}
