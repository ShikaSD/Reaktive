import com.badoo.reaktive.scheduler.Scheduler
import com.badoo.reaktive.scheduler.computationScheduler
import com.badoo.reaktive.scheduler.createNewThreadScheduler
import com.badoo.reaktive.scheduler.submit
import com.badoo.reaktive.single.blockingGet
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

private class ReaktiveContinuation<T>(
    private val wrapped: Continuation<T>,
    private val scheduler: Scheduler
) : Continuation<T> {
    override val context: CoroutineContext = wrapped.context

    override fun resumeWith(result: Result<T>) {
        scheduler.submit { wrapped.resumeWith(result) }
    }
}

class Reaktive(private val scheduler: Scheduler) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        ReaktiveContinuation(continuation, scheduler)
}

suspend fun <T> withContext(context: CoroutineContext, block: suspend () -> T): T =
    suspendCoroutine { originalCont ->
        val newCont = Continuation<T>(context) {
            originalCont.resumeWith(it)
        }
        block.startCoroutine(newCont)
    }

fun main() {
    val result = singleSuspended {
        withContext(Reaktive(createNewThreadScheduler())) {
            val value = withContext(Reaktive(computationScheduler)) {
                println("created value on ${Thread.currentThread()}")
                0
            }
            println("received value on ${Thread.currentThread()}, $value")
        }
    }.blockingGet()

    // prints
    // created value on Thread[Computation, pool-1-thread-2,5,main]
    // received value on Thread[NewThread, pool-1-thread-3,5,main], 0
}
