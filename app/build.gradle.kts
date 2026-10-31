plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "hu.toliver.muskli"
    compileSdk = 37

    defaultConfig {
        applicationId = "hu.toliver.muskli"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // The release workflow signs with ANDROID_KEYSTORE_* when those secrets are set.
    // Until a keystore is provided, release still builds, signed with the debug key.
    val releaseKeystore = System.getenv("ANDROID_KEYSTORE_FILE").orEmpty()
    signingConfigs {
        if (releaseKeystore.isNotEmpty()) {
            val storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            require(!storePassword.isNullOrEmpty()) {
                "ANDROID_KEYSTORE_FILE is set but ANDROID_KEYSTORE_PASSWORD is empty."
            }
            require(!keyAlias.isNullOrEmpty()) {
                "ANDROID_KEYSTORE_FILE is set but ANDROID_KEY_ALIAS is empty."
            }
            require(!keyPassword.isNullOrEmpty()) {
                "ANDROID_KEYSTORE_FILE is set but ANDROID_KEY_PASSWORD is empty."
            }
            create("release") {
                storeFile = file(releaseKeystore)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (releaseKeystore.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric's SDK 36 sandbox reflects into JDK internals that JDK 17+ does not export by default.
        unitTests.all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Declares the empty activity that createComposeRule launches.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
