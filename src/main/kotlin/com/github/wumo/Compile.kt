package com.github.wumo

import JavaCPPPluginExtension
import com.github.wumo.javacpp.Build
import com.github.wumo.javacpp.Presets
import com.github.wumo.javacpp.Target
import jniProjectBuild
import net.lingala.zip4j.ZipFile
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal fun Project.compileProject(
  jniProjectRoot: String,
  targets: List<Target>,
  preset: Presets,
  config: JavaCPPPluginExtension,
  resourceDir: String
) {
  val platform = Build.platform
  val jniBuild = "$jniProjectBuild/$platform"
  val cppbuildDirFile = file(jniBuild)
  delete(cppbuildDirFile)
  cppbuildDirFile.mkdirs()
  
  compiles {
    var compilerStr = ""
    if(config.c_compiler != null)
      compilerStr += " -DCMAKE_C_COMPILER=${config.c_compiler} "
    if(config.cxx_compiler != null)
      compilerStr += " -DCMAKE_CXX_COMPILER=${config.cxx_compiler} "
    run(
      cppbuildDirFile,
      "cmake $jniProjectRoot -DCMAKE_BUILD_TYPE=Release -G Ninja -DCMAKE_MAKE_PROGRAM=$ninja $compilerStr"
    )
    
    targets.forEach { (packagePath, jniLibName, link)->
      run(cppbuildDirFile, "cmake --build . --target $jniLibName --config Release")
      
      val targetDir = File(resourceDir).resolve(packagePath.replace('.', File.separatorChar)).path
      val files = link + jniLibName + preset.preload
      files.forEach {
        val targetName = "${Build.libraryPrefix}$it${Build.librarySuffix}"
        searchAndCopyTo(
          jniBuild, listOf("bin", "lib"), targetName,
          "$targetDir/$platform"
        )
      }
    }
  }
}

fun compiles(block: CompileEnv.()->Unit) {
  val (os, arch) = Build.platform.split('-')
  block(CompileEnv(os, arch))
}

class CompileEnv(os: String, arch: String) {
  private val isWindows = os == "windows"
  private var vcvarsall = ""
  private var vcvarsall_arch = when(arch) {
    "x86"    -> "x86"
    "x86_64" -> "x64"
    else     -> ""
  }
  val ninja: String
  
  init {
    val cacheDir = Paths.get(System.getProperty("user.home"), ".javacpp", "cache")
      .toAbsolutePath().toFile()
    cacheDir.mkdirs()
    if(isWindows) {
      val vswhere = downloadIfNotExists(
        cacheDir,
        "vswhere${Build.exeSuffix}",
        "https://github.com/microsoft/vswhere/releases/download/2.8.4/vswhere.exe"
      ).toString()
      check(File(vswhere).exists()){"error find vswhere"}
      val version = call("cmd", "/c", "\"$vswhere\" -latest -property installationVersion").trim()
      check(version.isNotBlank()) { "msvc is missing" }
      val (major) = version.split('.')
      val msvcRoot = call("cmd", "/c", "\"$vswhere\" -latest -property installationPath").trim()
      vcvarsall = Paths.get(msvcRoot, "VC/Auxiliary/Build/vcvarsall.bat").toString()
      check(File(vcvarsall).exists()) { "vcvarsall.bat is missing!" }
    }
    ninja =
      downloadIfNotExists(
        cacheDir,
        "ninja${Build.exeSuffix}",
        when(os) {
          "windows" -> "https://github.com/ninja-build/ninja/releases/download/v1.10.0/ninja-win.zip"
          "mac"     -> "https://github.com/ninja-build/ninja/releases/download/v1.10.0/ninja-mac.zip"
          "linux"   -> "https://github.com/ninja-build/ninja/releases/download/v1.10.0/ninja-linux.zip"
          else      -> error("not supported")
        },
        true
      ).toString()
  }
  
  fun run(workDir: File, cmd: String) {
    if(isWindows)
      workDir.exec("cmd", "/c", "call \"$vcvarsall\" $vcvarsall_arch && $cmd")
    else
      workDir.exec("/bin/bash", "-c", cmd)
  }
  
  private fun downloadIfNotExists(
    dstDir: File, exePath: String, url: String,
    unzip: Boolean = false
  ): File {
    val exe = dstDir.resolve(exePath)
    if(!exe.exists()) {
      val tmp = dstDir.resolve("$exePath.tmp")
      val conn = URL(url)
      conn.openStream().use { input->
        FileOutputStream(tmp).use { output->
          input.copyTo(output)
        }
      }
      if(unzip) {
        ZipFile(tmp).extractFile(exePath, dstDir.toString())
        tmp.delete()
      } else
        Files.move(tmp.toPath(), exe.toPath())
      println("downloaded $exe from $url")
    }
    return exe
  }
}

fun searchAndCopyTo(rootPath: String, dirs: List<String>, targetFile: String, targetDir: String) {
  var found = false
  for(dir in dirs) {
    val file = File("$rootPath/$dir/$targetFile")
    if(file.exists()) {
      file.copyTo(File("$targetDir/$targetFile"), true)
      found = true
      break
    }
  }
  check(found) { "Not found $targetFile" }
}