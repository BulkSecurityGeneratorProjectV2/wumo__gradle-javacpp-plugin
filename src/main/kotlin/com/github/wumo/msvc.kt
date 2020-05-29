package com.github.wumo

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Path

fun vcvarsall(workingDir: File, arch: String, cmd: String) {
  val exe = "vswhere.exe"
  if(!File(exe).exists())
    FileOutputStream(exe).use { out->
      Thread.currentThread().contextClassLoader
        .getResourceAsStream(exe).use {
          it!!.transferTo(out)
        }
    }
  
  val msvcRoot = call("cmd", "/c", "vswhere -latest -property installationPath").trim()
  val vcvarsall = Path.of(msvcRoot, "VC/Auxiliary/Build/vcvarsall.bat").toString()
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
    it.transferTo(System.out)
  }
}