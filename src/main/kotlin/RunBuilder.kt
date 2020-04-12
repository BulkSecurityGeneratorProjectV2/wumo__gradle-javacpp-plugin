import org.bytedeco.javacpp.tools.Build
import org.bytedeco.javacpp.tools.Target

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