package com.github.wumo

import com.github.wumo.javacpp.Build
import net.lingala.zip4j.ZipFile
import org.gradle.internal.impldep.com.google.common.io.Files
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun ensureExe(os: String) {
  if(os == "windows") {
    val vswhere = "vswhere${Build.exeSuffix}"
    downloadIfNotExists(
      vswhere,
      "https://github.com/microsoft/vswhere/releases/download/2.8.4/vswhere.exe"
    )
    val version = call("cmd", "/c", "vswhere -latest -property installationVersion").trim()
    check(version.isNotBlank()) { "msvc is missing" }
    val (major) = version.split('.')
  }
  val ninja = "ninja${Build.exeSuffix}"
  downloadIfNotExists(
    ninja,
    when(os) {
      "windows" -> "https://github.com/ninja-build/ninja/releases/download/v1.10.0/ninja-win.zip"
      "mac"     -> "https://github.com/ninja-build/ninja/releases/download/v1.10.0/ninja-mac.zip"
      "linux"   -> "https://github.com/ninja-build/ninja/releases/download/v1.10.0/ninja-linux.zip"
      else      -> error("not supported")
    },
    true
  )
}

private fun downloadIfNotExists(exePath: String, url: String, unzip: Boolean = false) {
  val exe = File(exePath)
  if(!exe.exists()) {
    val tmp = File("$exePath.tmp")
    val url = URL(url)
    url.openConnection()
    url.openStream().use { input->
      FileOutputStream(tmp).use { output->
        input.transferTo(output)
      }
    }
    if(unzip) {
      ZipFile(tmp).extractFile(exePath, ".")
      tmp.delete()
    } else
      Files.move(tmp, exe)
  }
}

