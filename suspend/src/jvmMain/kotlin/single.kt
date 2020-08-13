import com.badoo.reaktive.disposable.CompositeDisposable
import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.firstOrError
import com.badoo.reaktive.single.Single
import com.badoo.reaktive.single.SingleObserver
import com.badoo.reaktive.single.single
import com.badoo.reaktive.single.singleOf
import com.badoo.reaktive.single.subscribe
import com.badoo.reaktive.subject.publish.PublishSubject
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

fun main() {
    val subject = PublishSubject<String>()
    val disposable = singleSuspended {
        val value1 = subject.firstOrError().suspended()
        val value2 = singleOf("1").suspended()
        value1 + value2
    }.subscribe(
        onSuccess = { println(it) }
    )

    disposable.dispose()

    subject.onNext("0")

    // prints "01"
}

fun <T> singleSuspended(block: suspend () -> T): Single<T> =
    single { emitter ->
        val innerDisposable = CompositeDisposable()
        emitter.setDisposable(innerDisposable)

        block.startCoroutine(
            object : Continuation<T> {
                override val context: CoroutineContext = DisposableContext(innerDisposable)
                override fun resumeWith(result: Result<T>) {
                    if (emitter.isDisposed) return

                    result.fold(
                        onSuccess = { emitter.onSuccess(it) },
                        onFailure = { emitter.onError(it) }
                    )
                }
            }
        )
    }

suspend fun <T> Single<T>.suspended(): T =
    suspendCoroutine { cont ->
        subscribe(object : SingleObserver<T> {
            override fun onError(error: Throwable) {
                cont.resumeWith(Result.failure(error))
            }

            override fun onSubscribe(disposable: Disposable) {
                cont.context[DisposableContext]?.disposable?.add(disposable)
            }

            override fun onSuccess(value: T) {
                cont.resumeWith(Result.success(value))
            }
        })
    }

class DisposableContext(val disposable: CompositeDisposable) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<DisposableContext>
}
