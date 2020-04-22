import org.bytedeco.javacpp.Loader
import org.bytedeco.javacpp.tools.Info
import java.io.File
import java.util.*

fun main() {
  val properties: Properties = run {
    System.setProperty("org.bytedeco.javacpp.loadlibraries", "false")
    Loader.loadProperties()
  }
  properties.setProperty("platform.includepath", "cpp/NativeVideoPlayer/src")
  NParser(properties = properties)
    .parse(File("gen"), NativeVideoPlayer::class.java)

//  builder.property("platform.includepath", "cpp/NativeVideoPlayer/src")
//  builder.outputDirectory = File("gen")
//  builder.generateJava()
}