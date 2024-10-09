ThisBuild / scalaVersion := "2.13.12"  // Scala 版本
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"

lazy val root = (project in file("."))
  .settings(
    name := "SZ_fpga",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.6.0",    // Chisel 库
      "edu.berkeley.cs" %% "chiseltest" % "0.5.4" % Test,  // Chisel 测试库（可选）
      "org.scalameta" %% "munit" % "0.7.29" % Test, // 单元测试
      "org.scalatest" %% "scalatest" % "3.2.9" % Test  // 单元测试
    )
  )
