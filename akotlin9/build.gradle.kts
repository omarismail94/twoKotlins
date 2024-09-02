import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
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
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(project.projectDir.path)
                    }
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

val wasmTestArtifactAttribute = Attribute.of("com.example.wasm-test-artifact", String::class.java)

configurations {
    // Custom configuration for WASM test artifacts
    register("wasmTestArtifacts") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(wasmTestArtifactAttribute, "wasm-test")
        }
    }
}

artifacts {
    // Add the task output directly to the configuration
    add("wasmTestArtifacts", tasks.named("compileTestDevelopmentExecutableKotlinWasmJs").map {
        it.outputs.files.singleFile
    })
}
