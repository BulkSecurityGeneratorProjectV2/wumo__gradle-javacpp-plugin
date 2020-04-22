import org.bytedeco.javacpp.Loader
import org.bytedeco.javacpp.tools.Build
import org.bytedeco.javacpp.tools.Target
import java.io.File
import java.util.*

internal fun parse(includeDir: String, generateDir: String, preset: Presets) {
  System.setProperty("org.bytedeco.javacpp.loadlibraries", "false")
  val properties = Loader.loadProperties()
  properties.setProperty("platform.includepath", includeDir)
  
  val parser = MParser(properties = properties)
  parser.parse(File(generateDir), preset)
}

internal fun parse(includeDir: String, generateDir: String, vararg classPaths: String) {
  val b = Build().apply {
    classPaths(*classPaths)
    property("platform.includepath", includeDir)
    outputDirectory(generateDir)
  }
  b.generateJava()
}

internal fun generate(generatedJNISrc: String, vararg classPaths: String): List<Target> {
  val b = Build().apply {
    classPaths(*classPaths)
    outputDirectory(generatedJNISrc)
  }
  return b.generateJNI()
}