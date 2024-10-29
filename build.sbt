ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-language:reflectiveCalls",
  "-deprecation"
)

lazy val root = (project in file("."))
  .settings(
    name := "SZ_fpga",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.6.0",    // Chisel 库
      "edu.berkeley.cs" %% "chiseltest" % "0.6.1" % Test, // Chisel 测试库 (更新至兼容版本)
      "org.scalameta" %% "munit" % "0.7.29" % Test, // MUnit 测试库
      "org.scalatest" %% "scalatest" % "3.2.9" % Test // ScalaTest 测试库
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.6.0" cross CrossVersion.full), // Chisel 编译器插件
    resolvers ++= Seq(
      Resolver.sonatypeRepo("public"),
      Resolver.mavenLocal,
      "Maven Central" at "https://repo1.maven.org/maven2"
    ),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary
  )
