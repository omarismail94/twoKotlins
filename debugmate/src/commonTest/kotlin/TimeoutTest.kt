import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class TimeoutTest {

    @Test
    fun logPrintsOnFail() {
        println("this log prints")
        assertTrue { false }
    }

    @Test
    fun logDoesNotPrintOnTimeout() = runTest(timeout = 10.seconds) {
        println("this log does NOT print")
        val flow = flow {
            while (true) {
                emit(1)
            }
        }
        val job = launch { flow.collect() }
        job.start()
    }

    @Test
    fun logPrintOnTimeoutWorkaround() = runTest(timeout = 10.seconds) {
        println("this log prints")
        withTimeoutOrNull(100) {
            val flow = flow {
                while (true) {
                    println("I am inside")
                    emit(1)
                    delay(10)
                }
            }
            flow.collect()
        }
    }

    @Test
    fun logDoesPrintOnTimeout() = runTest(timeout = 10.seconds) {
        println("this log prints")
        val flow = flow {
            while (true) {
                emit(1)
                delay(10)
            }
        }
        val job = launch { flow.collect() }
        job.start()
    }
}