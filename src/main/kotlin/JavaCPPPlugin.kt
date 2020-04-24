import com.github.wumo.javacpp.Build
import com.github.wumo.javacpp.generate
import com.github.wumo.javacpp.parse
import org.bytedeco.javacpp.tools.InfoMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

import org.gradle.kotlin.dsl.*
import java.io.File
import java.lang.StringBuilder

open class JavaCPPPluginExtension {
  var include: List<String> = emptyList()
  var preload: List<String> = emptyList()
  var link: List<String> = emptyList()
  var target: String? = null
  var infoMap: (InfoMap) -> Unit = {}
  var cppSourceDir: String? = null
  var cppIncludeDir: String? = null
  var copyToResources: Pair<String, String>? = null
}

internal const val JAVACPP_NAME = "javacpp"

internal lateinit var config: JavaCPPPluginExtension

internal lateinit var include: List<String>
internal lateinit var preload: List<String>
internal lateinit var link: List<String>
internal lateinit var target: String
internal lateinit var infoMap: (InfoMap) -> Unit

internal lateinit var cppSource: String
internal lateinit var cppInclude: String
internal lateinit var generatedJavaSrc: String
internal lateinit var generatedJNISrc: String
internal lateinit var resourceDir: String
internal lateinit var jniSrc: String

fun Project.javacpp(block: JavaCPPPluginExtension.() -> Unit) {
  configure(block)
  include = config.include
  preload = config.preload
  link = config.link
  target = config.target ?: error("required to set target")
  infoMap = config.infoMap
  
  generatedJavaSrc = "$projectDir/src/main/java"
  cppSource = config.cppSourceDir ?: error("required to set source root folder")
  cppInclude = config.cppIncludeDir ?: error("required to set include folder")
  jniSrc = "$buildDir/cpp/jni${File(cppSource).name}"
  generatedJNISrc = "$jniSrc/src/jni"
}

class JavaCPPPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    config = extensions.create("config")
    plugins.apply(JavaLibraryPlugin::class)
    val main = convention.getPlugin<JavaPluginConvention>()
      .sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    generatedJavaSrc = main.java.srcDirs.first().path
    resourceDir = main.resources.srcDirs.first().path
    
    val javaCompile = tasks.getByName<JavaCompile>("compileJava")
    
    afterEvaluate {
      dependencies.add("api", "org.bytedeco:javacpp:1.5.3")
      dependencies.add("implementation", "org.bytedeco:javacpp-platform:1.5.3")
      
      val preset = Presets(
        include,
        preload,
        link,
        target,
        infoMap
      )
      
      val generateJava by tasks.registering {
        group = JAVACPP_NAME
        description = "Generate JNI Java code."
        
        outputs.dir(generatedJavaSrc)
        doLast {
          parse(
            cppInclude,
            generatedJavaSrc,
            preset
          )
        }
      }
      javaCompile.dependsOn(generateJava)
      val copyCPPSrc by tasks.registering(Copy::class) {
        group = JAVACPP_NAME
        from(cppSource)
        into(jniSrc)
        exclude {
          it.isDirectory && (it.name.startsWith("cmake-build") || it.name == "build" || it.name == ".idea")
        }
        eachFile {
          if (name == "CMakeLists.txt" && relativePath.parent.isEmpty())
            name = "CMakeListsOriginal.txt"
        }
      }
      val generateJNI by tasks.registering {
        group = JAVACPP_NAME
        description = "Generate JNI native code."
        dependsOn(copyCPPSrc)
        dependsOn(javaCompile)
        
        doLast {
          val targets = generate(
            generatedJNISrc,
            javaCompile.destinationDir.path
          )
          println(targets)
          val sb = StringBuilder()
          sb.append(
            """
cmake_minimum_required(VERSION 3.12)
project(${this@run.name} LANGUAGES C CXX)

include(CMakeListsOriginal.txt)
find_package(JNI REQUIRED)"""
          )
          
          val relative = File(generatedJNISrc).relativeTo(File(
            jniSrc
          ))
          targets.forEach { (_, jniLibName, link, cppFiles) ->
            sb.append(
              """
add_library($jniLibName SHARED
${cppFiles.joinToString("\n") { relative.resolve(it).path.replace(File.separatorChar, '/') }}
  )
target_include_directories($jniLibName PUBLIC ${'$'}{JNI_INCLUDE_DIRS})
target_compile_definitions($jniLibName PRIVATE _JNI_IMPLEMENTATION)
target_link_libraries($jniLibName PUBLIC ${link.joinToString(" ") { it }})
            """
            )
          }
          file("$jniSrc/CMakeLists.txt").writeText(sb.toString())
          
          val platform = Build.platform
          val jniBuild = "$buildDir/cpp/build/$platform"
          val cppbuildDirFile = file(jniBuild)
          delete(cppbuildDirFile)
          cppbuildDirFile.mkdirs()
          exec {
            workingDir = cppbuildDirFile
            commandLine(
              "cmake",
              jniSrc,
              "-DCMAKE_BUILD_TYPE=Release",
              "-DCMAKE_GENERATOR_PLATFORM=${if (platform.endsWith("-x86_64")) "x64" else "x86"}"
            )
          }
          targets.forEach { (packagePath, jniLibName, link) ->
            exec {
              workingDir = cppbuildDirFile
              commandLine("cmake", "--build", ".", "--target", jniLibName, "--config", "Release")
            }
            val targetDir = File(resourceDir).resolve(packagePath.replace('.', File.separatorChar)).path
            val files = link + jniLibName
            files.forEach {
              val targetName = "${Build.libraryPrefix}$it${Build.librarySuffix}"
              file("$jniBuild/bin/$targetName").copyTo(
                file("$targetDir/$platform/$targetName"),
                true
              )
            }
            preset.preload.forEach { lib ->
              val targetName = "${Build.libraryPrefix}$lib${Build.librarySuffix}"
              file("$jniBuild/bin/$targetName").copyTo(
                file("$targetDir/$platform/$targetName"),
                true
              )
            }
          }
        }
      }
      val copyResources by tasks.registering(Copy::class) {
        group = JAVACPP_NAME
        config.copyToResources?.let { (from, to) ->
          from("$jniSrc/$from")
          into("$resourceDir/$to")
        }
      }
      
      generateJNI.get().finalizedBy(copyResources)
//      javaCompile.finalizedBy(generateJNI)
    }
  }
}