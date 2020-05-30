package com.github.wumo

import org.gradle.api.Project
import com.github.wumo.javacpp.Target
import com.github.wumo.javacpp.generate
import java.io.File

internal fun Project.generateJNIProject(jniProjectRoot: String, srcDir: String, destDir: String): List<Target> {
  val targets = generate(srcDir, destDir)
  println(targets)
  val cmakelists = buildString {
    append(
      """cmake_minimum_required(VERSION 3.12)
project($name LANGUAGES C CXX)

include(CMakeListsOriginal.txt)
find_package(JNI REQUIRED)
"""
    )
    
    val relative = File(srcDir).relativeTo(File(jniProjectRoot))
    targets.forEach { (_, jniLibName, link, cppFiles)->
      appendln("add_library($jniLibName SHARED")
      cppFiles.forEach {
        appendln(relative.resolve(it).path.replace(File.separatorChar, '/'))
      }
      appendln(")")
      appendln(
        """target_include_directories($jniLibName PUBLIC ${'$'}{JNI_INCLUDE_DIRS})
target_compile_definitions($jniLibName PRIVATE _JNI_IMPLEMENTATION)
target_link_libraries($jniLibName PUBLIC ${link.joinToString(" ") { it }})"""
      )
      appendln()
    }
  }
  file("$jniProjectRoot/CMakeLists.txt").writeText(cmakelists)
  return targets
}