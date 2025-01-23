import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  kotlin("multiplatform") version "1.9.10"
  id("com.diffplug.spotless") version "6.25.0"
}

group = "com.serranofp"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
  explicitApi()

  targetHierarchy.default()

  jvm { jvmToolchain(8) }
  js {
    browser()
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("io.arrow-kt:arrow-core:1.2.4")
      }
    }
    val commonTest by getting
  }
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint()
  }
}
