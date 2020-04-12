plugins {
  `kotlin-dsl`
  `maven-publish`
}

group = "wumo"
version = "1.0.1"

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
}

dependencies {
  implementation(kotlin("reflect"))
  implementation("org.bytedeco:javacpp:1.5.2")
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