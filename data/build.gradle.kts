plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "hu.toliver.muskli.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))

    testImplementation(libs.junit4)
}
