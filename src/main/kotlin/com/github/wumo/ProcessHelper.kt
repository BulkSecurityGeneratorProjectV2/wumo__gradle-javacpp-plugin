package com.github.wumo

import com.github.wumo.javacpp.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Paths

fun vcvarsall(workingDir: File, arch: String, cmd: String) {
  val exe = "vswhere${Build.exeSuffix}"
//    FileOutputStream(exe).use { out->
//      Thread.currentThread().contextClassLoader
//        .getResourceAsStream(exe).use {
//          it!!.transferTo(out)
//        }
//    }
  
  val msvcRoot = call("cmd", "/c", "vswhere -latest -property installationPath").trim()
  val vcvarsall = Paths.get(msvcRoot, "VC/Auxiliary/Build/vcvarsall.bat").toString()
  check(File(vcvarsall).exists()) { "vcvarsall.bat is missing!" }
  val _arch = when(arch) {
    "x86"    -> "x86"
    "x86_64" -> "x64"
    else     -> error("not supported")
  }
  workingDir.exec("cmd", "/c", "call \"$vcvarsall\" $_arch && $cmd")
}

fun call(vararg command: String): String {
  val builder = ProcessBuilder(*command)
  builder.redirectErrorStream(true)
  val process: Process = builder.start()
  val input = process.inputStream
  val reader = BufferedReader(InputStreamReader(input))
  reader.use {
    return it.readText()
  }
}

fun File.exec(vararg command: String) {
  val builder = ProcessBuilder(*command)
  builder.directory(this)
  builder.redirectErrorStream(true)
  val process = builder.start()
  val input = process.inputStream
  input.use {
    it.copyTo(System.out)
  }
}