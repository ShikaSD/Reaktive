package com.badoo.reaktive.single

import com.badoo.reaktive.base.ErrorCallback
import com.badoo.reaktive.base.Observer
import com.badoo.reaktive.base.subscribeSafe
import com.badoo.reaktive.base.tryCatch
import com.badoo.reaktive.maybe.Maybe
import com.badoo.reaktive.maybe.maybeUnsafe

fun <T> Single<T>.filter(predicate: (T) -> Boolean): Maybe<T> =
    maybeUnsafe { observer ->
        subscribeSafe(
            object : SingleObserver<T>, Observer by observer, ErrorCallback by observer {
                override fun onSuccess(value: T) {
                    observer.tryCatch({ predicate(value) }) {
                        if (it) {
                            observer.onSuccess(value)
                        } else {
                            observer.onComplete()
                        }
                    }
                }
            }
        )
    }