import com.github.wumo.ensureExe
import com.github.wumo.javacpp.Build

fun main() {
  val (os, arch) = Build.platform.split('-')
  ensureExe(os)
}