import com.github.wumo.compileProject
import com.github.wumo.generateJNIProject
import com.github.wumo.javacpp.*
import com.github.wumo.javacpp.parse
import org.bytedeco.javacpp.tools.InfoMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import java.io.File

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
internal lateinit var jniProjectRoot: String
internal lateinit var jniProjectBuild: String
internal lateinit var jniBuild: String

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
  jniBuild = "$buildDir/cpp"
  jniProjectBuild = "$jniBuild/build"
  jniProjectRoot = "$jniBuild/jni${File(cppSource).name}"
  generatedJNISrc = "$jniProjectRoot/src/jni"
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

//        outputs.dir(generatedJavaSrc)
        doLast {
          parse(cppInclude, generatedJavaSrc, preset)
        }
      }
      javaCompile.dependsOn(generateJava)

      val clearCPP by tasks.registering(Delete::class) {
        group = JAVACPP_NAME
        delete(jniBuild)
      }

      val copyCPPSrc by tasks.registering(Copy::class) {
        dependsOn(clearCPP)

        group = JAVACPP_NAME
        delete(jniProjectRoot)
        from(cppSource)
        into(jniProjectRoot)
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
          val targets = generateJNIProject(
              jniProjectRoot,
              generatedJNISrc,
              javaCompile.destinationDir.path
          )

          compileProject(jniProjectRoot, targets, preset, config, resourceDir)
        }
      }

      val copyResources by tasks.registering(Copy::class) {
        group = JAVACPP_NAME
        config.copyToResources?.let { (from, to) ->
          from("$jniProjectRoot/$from")
          into("$resourceDir/$to")
        }
      }

      generateJNI.get().finalizedBy(copyResources)
    }
  }
}