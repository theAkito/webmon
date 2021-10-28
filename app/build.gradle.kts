import Build_gradle.Metadata.defaultSdkVersion
import Build_gradle.Metadata.kotlin_version
import Build_gradle.Metadata.ktx_version

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  id("kotlin-parcelize")
}

object Metadata {
  const val kotlin_version = "1.5.31"
  const val ktx_version = "1.7.0"
  const val defaultSdkVersion = 31
  val java8 = JavaVersion.VERSION_1_8
}

android {
  compileSdkVersion(defaultSdkVersion)
  buildToolsVersion("30.0.3")

  defaultConfig {
    configurations.all {
      resolutionStrategy { force("androidx.core:core-ktx:${ktx_version}") }
    }
    applicationId = "ooo.akito.webmon"
    minSdkVersion(19)
    targetSdkVersion(defaultSdkVersion)
    versionCode = 4
    versionName = "1.1.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    vectorDrawables {
      useSupportLibrary = true
    }

    kapt {
      arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
      }
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    viewBinding = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
  implementation("androidx.core:core-ktx:${ktx_version}")

  // Support Libraries & UI Components
  implementation("androidx.appcompat:appcompat:1.3.1")
  implementation("com.google.android.material:material:1.4.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.1")

  // Retrofit and relevant converters
  val retrofit_version = "2.9.0"
  api("com.squareup.retrofit2:retrofit:$retrofit_version")
  api("com.squareup.retrofit2:converter-gson:$retrofit_version")

  // Coroutines
  val coroutines_version = "1.4.1"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

  // Lifecycle
  implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
  implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")

  // Room
  val room_version = "2.4.0-beta01"
  implementation("androidx.room:room-runtime:$room_version")
  kapt("androidx.room:room-compiler:$room_version")
  implementation("androidx.room:room-ktx:$room_version")

  implementation("androidx.work:work-runtime-ktx:2.7.0")

  // Swipe refresh
  implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

  // Glide
  implementation("com.github.bumptech.glide:glide:4.11.0")

  // Testing
//  testImplementation("junit:junit:4.13.2")
//  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}