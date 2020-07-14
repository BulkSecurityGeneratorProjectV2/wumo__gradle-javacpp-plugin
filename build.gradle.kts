plugins {
  base
  `java-library`
  `kotlin-dsl`
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.11.0"
}

group = "com.github.wumo"
version = "1.0.11"

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

val publishPluginFromEnv by tasks.creating {
  tasks.publishPlugins.get().dependsOn(this)

  doLast {
    val content = File("${System.getProperty("user.home")}/.gradle/gradle.properties").readText()
    println(content)
  }
}

//val publishPluginFromEnv by tasks.creating {
//  val setupPublishEnv by tasks.creating {
//    doLast {
//      val key = System.getenv("GRADLE_PUBLISH_KEY") ?: error("GRADLE_PUBLISH_KEY not set")
//      val secret = System.getenv("GRADLE_PUBLISH_SECRET") ?: error("GRADLE_PUBLISH_SECRET not set")
//
//      System.setProperty("gradle.publish.key", key)
//      System.setProperty("gradle.publish.secret", secret)
//    }
//  }
//  tasks.login{
//    this.login()
//  }
//  dependsOn(setupPublishEnv)
//  dependsOn(tasks.publishPlugins.get())
//  tasks.publishPlugins.get().mustRunAfter(setupPublishEnv)
//}
repositories {
  mavenCentral()
}

dependencies {
  implementation("net.lingala.zip4j:zip4j:2.6.0")
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

