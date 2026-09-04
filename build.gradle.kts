plugins {
  kotlin("multiplatform") version "2.3.21"
  id("com.diffplug.spotless") version "8.10.2"
}

group = "com.serranofp"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(8)
  }
}

kotlin {
  explicitApi()

  applyDefaultHierarchyTemplate()

  jvm()
  js {
    browser()
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("io.arrow-kt:arrow-core:2.2.2.1")
      }
    }
    val commonTest by getting
  }

  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
  }
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint()
  }
}
