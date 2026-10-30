plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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

    // MigrationTestHelper reads the exported schemas as assets; Robolectric needs them on the test classpath.
    sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric's SDK 36 sandbox reflects into JDK internals that JDK 17+ does not export by default.
        unitTests.all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

/** Copies only the catalogue file, not the schema or tools beside it, so the APK carries the published bytes. */
abstract class CopyBundledCatalogue : DefaultTask() {
    @get:InputFile
    abstract val catalogue: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        catalogue.get().asFile.copyTo(out.resolve("catalogue/catalogue.json"))
    }
}

val copyBundledCatalogue = tasks.register<CopyBundledCatalogue>("copyBundledCatalogue") {
    catalogue.set(rootProject.layout.projectDirectory.file("catalogue/catalogue.json"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyBundledCatalogue, CopyBundledCatalogue::outputDir)
    }
}

dependencies {
    implementation(project(":domain"))

    // api: :app provides MuskliDatabase through Hilt, which needs RoomDatabase on its classpath.
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // api: :app creates the DataStore instance for Hilt.
    api(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
}
