import com.android.build.gradle.tasks.MergeSourceSetFolders

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
}

kotlin {
    jvmToolchain(21)
}
android {
    compileSdk = 35
    namespace =  "com.example.wasmtest"
    defaultConfig {
        applicationId = "com.example.wasmtest"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["webview-version"] = "130.0.6683.0"
    }

    sourceSets {
        named("androidTest") {
            assets.srcDirs(layout.buildDirectory.file("extra-assets/wasm"))
        }
    }
}

// Define the same custom attribute for resolving WASM test artifacts
val wasmTestArtifactAttribute = Attribute.of("com.example.wasm-test-artifact", String::class.java)

configurations {
    // Define a new configuration that will resolve the WASM test artifacts
    register("wasmTestArtifacts") {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(wasmTestArtifactAttribute, "wasm-test")
        }
    }
}

val copyWasmTestArtifacts = tasks.register<Copy>("copyWasmTestArtifacts") {
    from(configurations.named("wasmTestArtifacts")) {
        include("*.wasm", "*.uninstantiated.mjs")
    }
    into(layout.buildDirectory.dir("extra-assets/wasm"))
    rename { fileName ->
        val extension = fileName.substringAfterLast(".")
        "wasm-test.$extension"
    }
}

tasks.withType(MergeSourceSetFolders::class.java).configureEach {
    dependsOn(copyWasmTestArtifacts)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    "wasmTestArtifacts"(project(":akotlin9"))
    androidTestImplementation(libs.javascriptengine)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.testRunner)
}
