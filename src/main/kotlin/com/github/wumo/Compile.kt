package com.github.wumo

import JavaCPPPluginExtension
import com.github.wumo.javacpp.Build
import com.github.wumo.javacpp.Presets
import com.github.wumo.javacpp.Target
import org.gradle.api.Project
import java.io.File
import java.nio.file.Path

internal fun Project.compileProject(
  jniProjectRoot: String,
  targets: List<Target>,
  preset: Presets,
  config: JavaCPPPluginExtension,
  resourceDir: String
) {
  val platform = Build.platform
  val jniBuild = "$buildDir/cpp/build/$platform"
  val cppbuildDirFile = file(jniBuild)
  delete(cppbuildDirFile)
  cppbuildDirFile.mkdirs()
  
  compiles {
    run(cppbuildDirFile, "cmake $jniProjectRoot -DCMAKE_BUILD_TYPE=Release -G Ninja")
    
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
  
  init {
    ensureExe(os)
    if(isWindows) {
      val msvcRoot = call("cmd", "/c", "vswhere -latest -property installationPath").trim()
      vcvarsall = Path.of(msvcRoot, "VC/Auxiliary/Build/vcvarsall.bat").toString()
      check(File(vcvarsall).exists()) { "vcvarsall.bat is missing!" }
    }
  }
  
  fun run(workDir: File, cmd: String) {
    if(isWindows)
      workDir.exec("cmd", "/c", "call \"$vcvarsall\" $vcvarsall_arch && $cmd")
    else
      workDir.exec(cmd)
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