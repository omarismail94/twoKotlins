import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

configurations.configureEach {
    resolutionStrategy {
        eachDependency {
            if (
                requested.group == "org.jetbrains.kotlin" &&
                requested.version == null
            ) {
                useVersion("1.9.24")
            }
        }
    }
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

        wasmJsMain.dependencies {
            implementation(project(":akotlin7"))
        }
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

afterEvaluate {
    kotlinExtension.coreLibrariesVersion = "1.9.24"
}
