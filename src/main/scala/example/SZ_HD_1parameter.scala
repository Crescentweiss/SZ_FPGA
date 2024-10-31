package example

import chisel3._
import chisel3.util._

object SZHDParameters {
  // 定义参数和变量
  val N = 17 // 2^n + 1, input data size <= N
  val eb = 10.S(32.W) // Example value for error bound
  val eb_2 = eb * 2.S(32.W)
}

trait SZHDVariables {
  // 引入参数
  import SZHDParameters._

  // 定义需要的变量
  val dataset = Reg(Vec(N, SInt(32.W)))
  val datadelta = RegInit(VecInit(Seq.fill(N)(0.S(32.W))))
  val dataquantization = RegInit(VecInit(Seq.fill(N)(0.S(16.W))))
  val datareconstruction = RegInit(VecInit(Seq.fill(N)(0.S(32.W))))
  val flagreconstruction = RegInit(VecInit(Seq.fill(N)(false.B)))
  val datacompression = RegInit(VecInit(Seq.fill(N)(0.U(32.W))))
  val lastbits = RegInit(0.U(6.W))
  val datadecompression = RegInit(VecInit(Seq.fill(N)(0.S(16.W))))
  val datasetrecover = RegInit(VecInit(Seq.fill(N)(0.S(32.W))))
  val flagrecover = RegInit(VecInit(Seq.fill(N)(false.B)))
}
