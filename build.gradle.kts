// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  `kotlin-dsl`
}

buildscript {
  val kotlin_version = "1.5.31"
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.1.3")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")

    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle files
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
  }
}