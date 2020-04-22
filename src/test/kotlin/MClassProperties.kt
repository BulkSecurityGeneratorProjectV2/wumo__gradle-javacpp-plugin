import org.bytedeco.javacpp.ClassProperties
import java.util.*

object MClassPropertiesLoader {
  
  fun load(properties: Properties, preset: Presets): ClassProperties {
    val cp = ClassProperties(properties)
    
    val target = preset.target
    val global = target
    val ourTarget = global
    
    if (target.isNotEmpty()) cp.addAll("target", target)
    if (global.isNotEmpty()) cp.addAll("global", global)
    
    val hasPlatformProperties = true
    
    val library = "jni" + ourTarget.substring(ourTarget.lastIndexOf('.') + 1)
    
    val include = preset.include
    val link = preset.link
    val preload = preset.preload
    
    cp.addAll("platform.pragma")
    cp.addAll("platform.define")
    cp.addAll("platform.exclude")
    cp.addAll("platform.include", include)
    cp.addAll("platform.cinclude")
    cp.addAll("platform.includepath")
    cp.addAll("platform.includeresource")
    cp.addAll("platform.compiler.*")
    cp.addAll("platform.linkpath")
    cp.addAll("platform.linkresource")
    cp.addAll("platform.link", link)
    cp.addAll("platform.frameworkpath")
    cp.addAll("platform.framework")
    cp.addAll("platform.preloadresource")
    cp.addAll("platform.preloadpath")
    cp.addAll("platform.preload", preload)
    cp.addAll("platform.resourcepath")
    cp.addAll("platform.resource")
    if (cp.platformExtension == null || cp.platformExtension.length == 0) {
      // don't override the platform extension when found outside the class
      cp.addAll("platform.extension")
    }
    cp.addAll("platform.executablepath")
    cp.setProperty("platform.executable", "")
    cp.setProperty("platform.library", library)
    
    cp.loaded = cp.loaded or hasPlatformProperties
    return cp
  }
}