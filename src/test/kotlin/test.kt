import com.github.wumo.javacpp.MParser
import com.github.wumo.javacpp.Presets
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
  val preset = Presets(
    listOf("main.h", "player.h", "input.h"),
    emptyList(),
    listOf("NativeVideoPlayer"),
    "com.github.waahoo.videoplayer.NativeVideoPlayer"
  ) {
    it.put(Info("waahoo::InputStreamCallback").virtualize())
      .put(Info("waahoo::FrameGetter").virtualize())
  }
  MParser(properties = properties)
    .parse(File("gen"), preset)

//  builder.property("platform.includepath", "cpp/NativeVideoPlayer/src")
//  builder.outputDirectory = File("gen")
//  builder.generateJava()
}