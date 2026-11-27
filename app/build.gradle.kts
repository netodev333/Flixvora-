import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.flixvora"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val keystorePropertiesFile = rootProject.file("key.properties")
  val keystoreProperties = Properties()
  if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
  }

  // Third-party API credentials can be rotated without touching code: override
  // them in gradle.properties or with -PtmdbApiKey1=... at build time. The
  // fallbacks below keep local and CI builds working out of the box.
  val tmdbApiKey1 = (project.findProperty("tmdbApiKey1") as String?) ?: "3e20e76d6d210b6cb128d17d233b64dc"
  val tmdbApiKey2 = (project.findProperty("tmdbApiKey2") as String?) ?: "203f18d6bdc16a8e9a1fa73a6c6da6bc"
  val openSubtitlesApiKey = (project.findProperty("openSubtitlesApiKey") as String?) ?: "ZRN9QpqcTxje5UH3bV1RuugwitxkqB53"

  // Self-hosted (non-GitHub) endpoint that serves the update manifest. Override
  // with -PupdateManifestUrl=https://... at build time; no code change needed.
  val updateManifestUrl = (project.findProperty("updateManifestUrl") as String?) ?: "https://api.github.com/repos/netodev333/Flixvora-/releases/latest"
  // Fallback: GitHub releases list endpoint
  val updateManifestUrl2 = (project.findProperty("updateManifestUrl2") as String?) ?: "https://api.github.com/repos/netodev333/Flixvora-/releases"

  // Single source of truth for the version. GitHub Actions reads the same
  // file to tag the release, so the APK, the release tag and the Me tab can
  // never drift apart again.
  val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) file.inputStream().use { load(it) }
  }
  val appVersionName = (project.findProperty("versionName") as String?)
    ?: versionProps.getProperty("versionName", "11.5")
  val appVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull()
    ?: versionProps.getProperty("versionCode", "64").toIntOrNull()
    ?: 64

  defaultConfig {
    applicationId = "com.flixvora.app"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName

    buildConfigField("String", "TMDB_API_KEY_1", "\"$tmdbApiKey1\"")
    buildConfigField("String", "TMDB_API_KEY_2", "\"$tmdbApiKey2\"")
    buildConfigField("String", "OPENSUBTITLES_API_KEY", "\"$openSubtitlesApiKey\"")
    // GitHub Releases API for update manifest
    buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")
    buildConfigField("String", "UPDATE_MANIFEST_URL_2", "\"$updateManifestUrl2\"")
  }

  signingConfigs {
    create("release") {
      val keyStorePath = keystoreProperties.getProperty("storeFile") ?: "release-key.jks"
      val resolvedFile = File(keyStorePath)
      storeFile = if (resolvedFile.isAbsolute) resolvedFile else rootProject.file(keyStorePath)
      storePassword = keystoreProperties.getProperty("storePassword")
      keyAlias = keystoreProperties.getProperty("keyAlias")
      keyPassword = keystoreProperties.getProperty("keyPassword")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
    }
  }
  lint {
    checkReleaseBuilds = false
    abortOnError = false
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



// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation("androidx.appcompat:appcompat:1.6.1")
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  implementation(libs.androidx.media3.exoplayer)
  implementation("androidx.media3:media3-session:1.4.1")
  implementation("androidx.work:work-runtime-ktx:2.10.0")
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
  implementation("androidx.mediarouter:mediarouter:1.7.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation(libs.converter.moshi)
  testImplementation(libs.okhttp)
}
