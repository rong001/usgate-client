plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.usgate.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.usgate.client"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "0.2.1-branded"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Device + emulator; drops armeabi-v7a / x86 to shrink APK.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Download libbox.aar at build time (not committed). Override with:
//   -Dlibbox.aar.url=...  or place file manually in app/libs/libbox.aar
val libboxVersion = (project.findProperty("libbox.version") as String?) ?: "1.13.14"
val libboxUrl = (project.findProperty("libbox.aar.url") as String?)
    ?: "https://jitpack.io/com/github/singbox-android/libbox/$libboxVersion/libbox-$libboxVersion.aar"
val libboxAar = layout.projectDirectory.file("libs/libbox.aar")

val fetchLibbox by tasks.registering {
    group = "usgate"
    description = "Download libbox.aar into app/libs/ (skipped if already present)"
    outputs.file(libboxAar)
    onlyIf { !libboxAar.asFile.exists() }
    doLast {
        val dest = libboxAar.asFile
        dest.parentFile.mkdirs()
        logger.lifecycle("Downloading libbox $libboxVersion from $libboxUrl")
        ant.invokeMethod(
            "get",
            mapOf(
                "src" to libboxUrl,
                "dest" to dest,
                "skipexisting" to false
            )
        )
        require(dest.exists() && dest.length() > 1_000_000) {
            "libbox.aar download failed or too small. Place AAR manually at ${dest.absolutePath}"
        }
        logger.lifecycle("Saved ${dest.absolutePath} (${dest.length()} bytes)")
    }
}

tasks.named("preBuild").configure {
    dependsOn(fetchLibbox)
}

dependencies {
    // Local AAR (fetched by :app:fetchLibbox or scripts/fetch-libbox.sh)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.24")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
}
