import org.bytedeco.javacpp.tools.InfoMap

open class JavaCPPPluginExtension {
  var include: List<String> = emptyList()
  var preload: List<String> = emptyList()
  var link: List<String> = emptyList()
  var target: String? = null
  var infoMap: (InfoMap)->Unit = {}
  var cppSourceDir: String? = null
  var cppIncludeDir: String? = null
  var copyToResources: Pair<String, String>? = null
  var c_compiler: String? = null
  var cxx_compiler: String? = null
}