plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.github.sonatadev.sbldb"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.github.sonatadev.sbldb"
        minSdk = 24
        targetSdk = 37
        versionCode = 20
        versionName = "0.19.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The release key lives off the repo (GitHub secrets on CI, ~/sbldb-private on the VM).
    // Without it, release builds fall back to the debug key so they still build locally.
    val releaseKeystore = System.getenv("SBLDB_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SBLDB_KEYSTORE_PASSWORD")
                keyAlias = "sbldb"
                keyPassword = System.getenv("SBLDB_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    lint {
        // Every string needs its Italian version too
        error += "MissingTranslation"
    }
    buildFeatures {
        compose = true
    }
    // Exported Room schemas, needed by the migration tests
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

// Content tests read the YAML in src/main/assets directly, so edits there must rerun them
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/assets")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.snakeyaml)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.documentfile)
    // Aligns Navigation's kotlinx-serialization with the version room-testing is built against
    implementation(platform(libs.kotlinx.serialization.bom))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}