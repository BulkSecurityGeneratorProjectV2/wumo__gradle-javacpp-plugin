plugins {
  `kotlin-dsl`
  `maven-publish`
}

group = "com.github.wumo"
version = "1.0.3"

gradlePlugin {
  plugins {
    register("JavaCPPPlugin") {
      id = "wumo.javacpp"
      implementationClass = "JavaCPPPlugin"
    }
  }
}

repositories {
  mavenCentral()
  maven(url = "https://jitpack.io")
}

dependencies {
  implementation(kotlin("reflect"))
  implementation("com.github.wumo-hack:javacpp:1.5.4")
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