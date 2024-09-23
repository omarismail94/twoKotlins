import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "akotlin9"
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val wasmJsMain by getting
        val wasmJsTest by getting

        wasmJsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.configureEach {
    if (this is KotlinCompilationTask<*>) {
        this.compilerOptions.apiVersion.set(KotlinVersion.KOTLIN_1_9)
        this.compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_9)
    }
}

tasks.named(
    "wasmJsTestTestDevelopmentExecutableCompileSync",
    DefaultIncrementalSyncTask::class.java
) {
    destinationDirectory.set(
        file(layout.buildDirectory.dir("wasm-js-test/dev/kotlin"))
    )
}
