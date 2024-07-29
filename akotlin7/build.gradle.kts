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
                useVersion("1.7.10")
            }
            // I have to uncomment this to stop Gradle choosing version "kotlin-test-js-runner" 1.7.10
//            if(requested.module.name == "kotlin-test-js-runner") {
//                useVersion(
//                    project.extensions.getByType(VersionCatalogsExtension::class.java)
//                        .find("libs").get()
//                        .findVersion("kotlin").get()
//                        .requiredVersion
//                )
//            }
        }
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "akotlin7"
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

        commonMain.dependencies {
            api(libs.kotlin.stdlib)

        }

        wasmJsMain.dependencies {
            // Needed because there is no stdlib-wasm in Kotlin 1.7
            implementation(libs.kotlinStdlibJs)

        }
    }
}

tasks.configureEach {
    if (this is KotlinCompilationTask<*>) {
        this.compilerOptions.apiVersion.set(KotlinVersion.KOTLIN_1_7)
        this.compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_7)
    }
}

afterEvaluate {
        kotlinExtension.coreLibrariesVersion = "1.7.10"
}
