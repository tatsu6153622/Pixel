plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.aistudio.chronocanvas.wear"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.chronocanvas.pw3studio"
    minSdk = 30
    targetSdk = 36
    versionCode = 4
    versionName = "1.3"
  }

  signingConfigs {
    create("debugConfig") {
      val rootKeystore = file("${rootDir}/debug.keystore")
      if (rootKeystore.exists()) {
        storeFile = rootKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    debug {
      if (file("${rootDir}/debug.keystore").exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(project(":shared"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.play.services.wearable)
  implementation(libs.androidx.wear.remote.interactions)
  implementation(libs.androidx.wear)
  implementation(libs.androidx.wear.ongoing)
  implementation(libs.androidx.wear.tiles)
  implementation(libs.androidx.wear.protolayout)
  implementation(libs.androidx.wear.protolayout.material3)
  implementation(libs.androidx.wear.protolayout.expression)
  implementation(libs.androidx.wear.watchface.complications)
  implementation(libs.kotlinx.coroutines.android)
}
