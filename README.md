![Release](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/wumo/gradle-javacpp-plugin/maven-metadata.xml.svg?label=com.github.wumo.javacpp)

This plugin is one simplified [javacpp](https://github.com/bytedeco/javacpp) plugin that compiles [conan](https://conan.io/) based cpp project. Check the official [gradle-javacpp](https://github.com/bytedeco/gradle-javacpp).

## Usage
In your `build.gradle.kts`:
```kotlin
plugins {
  id("com.github.wumo.javacpp") version "1.0.7"
}

javacpp {
  include = listOf("header1.h", "header2.h")
  preload = listOf("lib1","lib2")
  link = listOf("MyLib")
  target = "com.example1.MyClass"
  infoMap = {
    it.put(Info("examples::Callback").virtualize())
      .put(Info("examples::Callback2").virtualize())
  }
  cppSourceDir = "${project.projectDir}/src/main/cpp/myLib"
  cppIncludeDir = "$cppSourceDir/src"
}
```
