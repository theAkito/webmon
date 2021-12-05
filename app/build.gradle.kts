import Build_gradle.Metadata.defaultSdkVersion
import Build_gradle.Metadata.kotlin_version
import Build_gradle.Metadata.ktx_version

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  id("kotlin-parcelize")
  id("kotlin-android")
}

object Metadata {
  const val kotlin_version = "1.5.31"
  const val ktx_version = "1.7.0"
  const val defaultSdkVersion = 31
  val java8 = JavaVersion.VERSION_1_8
}

android {
  compileSdk = defaultSdkVersion
  buildToolsVersion = "30.0.3"

  defaultConfig {
    configurations.all {
      resolutionStrategy { force("androidx.core:core-ktx:${ktx_version}") }
    }
    applicationId = "ooo.akito.webmon"
    minSdk = 19
    targetSdk = defaultSdkVersion
    versionCode = 12
    versionName = "2.7.0"

    multiDexEnabled = true

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

  /**
    Required by Apache HttpClient.
  */
  packagingOptions {
    resources.excludes.addAll(
      listOf(
        "META-INF/DEPENDENCIES",
        "META-INF/NOTICE",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/NOTICE.txt"
      )
    )
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
  implementation("androidx.core:core-ktx:${ktx_version}")

  // Main
  /** https://stackoverflow.com/a/69970914/7061105 */
  val main_version = "1.4.0"
  implementation("androidx.activity:activity-ktx:$main_version")
  implementation("androidx.fragment:fragment-ktx:$main_version")

  // Support Libraries & UI Components
  implementation("androidx.appcompat:appcompat:1.4.0")
  implementation("com.google.android.material:material:1.4.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.2")
  implementation("com.android.support:drawerlayout:28.0.0")

  // Retrofit and Relevant Converters
  val retrofit_version = "2.9.0"
  api("com.squareup.retrofit2:retrofit:$retrofit_version")
  api("com.squareup.retrofit2:converter-gson:$retrofit_version")

  // Coroutines
  val coroutines_version = "1.4.1"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

  // Lifecycle
  val lifecycle_version = "2.4.0"
  implementation("androidx.lifecycle:lifecycle-process:$lifecycle_version")
  implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
  /** https://developer.android.com/kotlin/ktx#livedata */
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
  /** https://stackoverflow.com/a/61498180/7061105 */
  /** https://github.com/googlecodelabs/android-room-with-a-view/issues/114#issuecomment-944479270 */
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
  implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycle_version")

  // Room
  val room_version = "2.4.0-beta01"
  implementation("androidx.room:room-runtime:$room_version")
  kapt("androidx.room:room-compiler:$room_version")
  implementation("androidx.room:room-ktx:$room_version")

  implementation("androidx.work:work-runtime-ktx:2.7.1")

  // Swipe Refresh
  implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

  // Glide
  implementation("com.github.bumptech.glide:glide:4.11.0")

  // JSON
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
  implementation("com.fasterxml.jackson.core:jackson-core:2.13")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.13")

  /**
    Apache HttpClient.
    https://ok2c.github.io/httpclient-android-ext/hc5.html
  */
  implementation("com.github.ok2c.hc5.android:httpclient-android:0.1.1")

  /** DNS Tool */
  implementation("org.minidns:minidns-hla:1.0.0")

  // Testing
  //testImplementation("junit:junit:4.13.2")
  //androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
  /* https://stackoverflow.com/a/61586889/7061105 */
  androidTestImplementation("androidx.work:work-testing:2.7.1")

}