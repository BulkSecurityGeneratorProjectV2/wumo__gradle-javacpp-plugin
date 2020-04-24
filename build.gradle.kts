plugins {
  `kotlin-dsl`
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.11.0"
}

group = "com.github.wumo"
version = "1.0.6"

pluginBundle {
  website = "https://github.com/wumo/gradle-javacpp-plugin"
  vcsUrl = "https://github.com/wumo/gradle-javacpp-plugin.git"
  tags = listOf("javacpp", "kotlin")
}

gradlePlugin {
  plugins {
    register("JavaCPPPlugin") {
      id = "com.github.wumo.javacpp"
      displayName = "javacpp"
      description = "simplified javacpp plugin"
      implementationClass = "JavaCPPPlugin"
    }
  }
}

repositories {
  mavenCentral()
  maven(url = "https://jitpack.io")
  mavenLocal()
}

dependencies {
  implementation(kotlin("reflect"))
  implementation("com.github.wumo-hack:javacpp:0.0.1")
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}

val kotlinSourcesJar by tasks

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
      artifact(kotlinSourcesJar)
    }
  }
}