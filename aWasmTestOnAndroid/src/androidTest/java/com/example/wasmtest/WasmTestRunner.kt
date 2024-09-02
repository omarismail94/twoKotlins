package com.example.wasmtest

import androidx.javascriptengine.JavaScriptSandbox
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WasmTestRunner {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val suitePattern = Regex("##teamcity\\[testSuiteStarted name='(.*?)' flowId='(.*?)'\\]")
    private val testFailedPattern =
        Regex(
            "##teamcity\\[testFailed name='(.*?)' (?:message='.*?')? details='(.*?)' flowId='(.*?)'\\]"
        )

    @Before
    fun setUp() {
        Assume.assumeTrue(JavaScriptSandbox.isSupported())
        val jsSandboxFuture = JavaScriptSandbox.createConnectedInstanceAsync(context)
        jsSandboxFuture[5, TimeUnit.SECONDS].use { jsSandbox ->
            jsSandbox.createIsolate().use {
                Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN)
                )
                Assume.assumeTrue(
                    jsSandbox.isFeatureSupported(
                        JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER
                    )
                )
            }
        }
    }

    @Test
    fun runWasmTests() {
        val wasmBase64 = context.assets.open("wasm-test.wasm").use { it.readBytes() }
        val uninstatiatedMjs =
            context.assets.open("wasm-test.mjs").bufferedReader().use { it.readText() }

        val jsCodeConst = extractJsCodeConst(uninstatiatedMjs)

        val script =
            """
            $jsCodeConst;
            let wasmExports;
            (async () => {
                const wasm = await android.consumeNamedDataAsArrayBuffer('wasm-test');
                const instance = await WebAssembly.instantiate(wasm, { js_code });
                wasmExports = instance.instance.exports;
                wasmExports._initialize();
                return wasmExports.startUnitTests();
            })()
        """
                .trimIndent()

        val jsSandbox =
            JavaScriptSandbox.createConnectedInstanceAsync(context).get(5, TimeUnit.SECONDS)

        val outputBuilder = StringBuilder()

        jsSandbox.use {
            it.createIsolate().use { jsIsolate ->
                jsIsolate.provideNamedData("wasm-test", wasmBase64)
                jsIsolate.setConsoleCallback { outputBuilder.appendLine(it.message) }
                jsIsolate.evaluateJavaScriptAsync(script).get()

                val failedTests = extractFailedTests(outputBuilder.toString())
                Assert.assertTrue(buildFailureMessage(failedTests), failedTests.isEmpty())
            }
        }
    }

    private fun extractFailedTests(output: String): List<String> {
        val failedTests = mutableListOf<String>()
        var currentSuiteName: String? = null

        output.lines().forEach { line ->
            suitePattern.find(line)?.let { match -> currentSuiteName = match.groupValues[1] }

            testFailedPattern.find(line)?.let { match ->
                val testName = match.groupValues[1]
                val details = match.groupValues[2].replace("|n", "\n").replace("|r", "\r")
                val fullTestName = currentSuiteName?.let { "$it.$testName" } ?: testName
                failedTests.add("Test: $fullTestName\n$details\n")
            }
        }

        return failedTests
    }

    private fun buildFailureMessage(failedTests: List<String>): String {
        return if (failedTests.isEmpty()) {
            "All tests passed."
        } else {
            buildString {
                append("Failed tests found:\n")
                failedTests.forEach { append(it).append("\n") }
            }
        }
    }

    private fun extractJsCodeConst(jsContent: String): String {
        val startMarker = "const js_code = {"
        val startIndex = jsContent.indexOf(startMarker)

        require(startIndex != -1) { "js_code not found" }

        var endIndex = startIndex + startMarker.length
        var braceCount = 1

        while (endIndex < jsContent.length && braceCount > 0) {
            when (jsContent[endIndex]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            endIndex++
        }

        return jsContent.substring(startIndex, endIndex)
    }
}
