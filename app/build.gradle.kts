import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun gitOutput(vararg args: String): String = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
} catch (_: Exception) {
    ""
}

val buildEnvOverride = providers.gradleProperty("buildEnv").orNull
val gitBranchName = gitOutput("rev-parse", "--abbrev-ref", "HEAD")
val buildEnvironment = buildEnvOverride ?: when {
    gitBranchName.equals("main", ignoreCase = true) ||
        gitBranchName.equals("master", ignoreCase = true) -> "main"
    gitBranchName.equals("test", ignoreCase = true) -> "test"
    gitBranchName.isNotBlank() -> "dev"
    else -> "local"
}
val gitShortSha = gitOutput("rev-parse", "--short", "HEAD").ifBlank { "local" }

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val oneNoteClientId = localProperties.getProperty("onenote.clientId", "").trim()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val oneNoteSyncAvailable = localProperties.getProperty("onenote.clientId", "").trim().isNotEmpty()

val sideloadPropertiesFile = rootProject.file("keystore/sideload.properties")
val sideloadProperties = Properties().apply {
    if (sideloadPropertiesFile.exists()) {
        sideloadPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.meditationparticles"
    compileSdk = 35

    signingConfigs {
        create("sideload") {
            storeFile = rootProject.file(
                sideloadProperties.getProperty("storeFile") ?: "keystore/sideload.jks",
            )
            storePassword = sideloadProperties.getProperty("storePassword")
            keyAlias = sideloadProperties.getProperty("keyAlias")
            keyPassword = sideloadProperties.getProperty("keyPassword")
        }
    }

    defaultConfig {
        applicationId = "com.example.meditationparticles"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.0.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("Boolean", "UPDATE_CHECK_ENABLED", "true")
        buildConfigField(
            "String",
            "UPDATE_MANIFEST_URL",
            "\"https://raw.githubusercontent.com/mpburton812/serene-interval/main/release/version.json\"",
        )
        buildConfigField("String", "BUILD_ENV", "\"$buildEnvironment\"")
        buildConfigField("String", "GIT_SHA", "\"$gitShortSha\"")
        buildConfigField("String", "SHORT_BUILD_LABEL", "\"$gitShortSha ($buildEnvironment)\"")
        buildConfigField("String", "ONENOTE_CLIENT_ID", "\"$oneNoteClientId\"")
        buildConfigField("Boolean", "ONENOTE_SYNC_AVAILABLE", "$oneNoteSyncAvailable")
        buildConfigField(
            "String",
            "ONENOTE_REDIRECT_SIGNATURE_HASH",
            "\"wnyLuNCKNp-EU4eMI6tuS0f-G_I\"",
        )
    }

    buildTypes {
        debug {
            // Shared sideload key so installDebug and GitHub update APKs match.
            signingConfig = signingConfigs.getByName("sideload")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.msal)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
