import org.bytedeco.javacpp.annotation.Platform
import org.bytedeco.javacpp.annotation.Properties
import org.bytedeco.javacpp.tools.Info
import org.bytedeco.javacpp.tools.InfoMap
import org.bytedeco.javacpp.tools.InfoMapper

@Properties(
  value = [Platform(
    include = ["main.h", "player.h", "input.h"],
    link = ["NativeVideoPlayer"]
  )], target = "com.github.waahoo.videoplayer.NativeVideoPlayer"
)
class NativeVideoPlayer : InfoMapper {
  override fun map(infoMap: InfoMap) {
    infoMap.put(Info("waahoo::InputStreamCallback").virtualize())
      .put(Info("waahoo::FrameGetter").virtualize())
  }
}