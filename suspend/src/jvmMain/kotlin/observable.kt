import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.single.subscribe
import com.badoo.reaktive.subject.publish.PublishSubject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun <T> Observable<T>.suspended(onReceive: (T) -> Unit) {
    suspendCoroutine<Unit> { cont ->
        subscribe(object : ObservableObserver<T> {
            override fun onComplete() {
                cont.resume(Unit)
            }

            override fun onError(error: Throwable) {
                cont.resumeWith(Result.failure(error))
            }

            override fun onSubscribe(disposable: Disposable) {
                cont.context[DisposableContext]?.disposable?.add(disposable)
            }

            override fun onNext(value: T) {
                onReceive(value)
            }
        })
    }
}

fun main() {
    val subject = PublishSubject<String>()

    val disposable = singleSuspended {
        var value = ""
        subject.suspended { value += it  }
        value
    }.subscribe { println(it) }

    subject.onNext("1")
    subject.onNext("2")
    subject.onNext("3")
    subject.onComplete()

    // prints "123"
}
