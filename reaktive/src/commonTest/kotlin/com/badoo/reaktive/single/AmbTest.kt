package com.badoo.reaktive.single

import com.badoo.reaktive.test.base.assertError
import com.badoo.reaktive.test.base.assertNotError
import com.badoo.reaktive.test.base.hasSubscribers
import com.badoo.reaktive.test.single.TestSingle
import com.badoo.reaktive.test.single.assertSuccess
import com.badoo.reaktive.test.single.test
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AmbTest {

    private val source1 = TestSingle<Int?>()
    private val source2 = TestSingle<Int?>()
    private val source3 = TestSingle<Int?>()
    private val amb = amb(source1, source2, source3).test()

    @Test
    fun all_sources_subscribed() {
        assertTrue(source1.hasSubscribers)
        assertTrue(source2.hasSubscribers)
        assertTrue(source3.hasSubscribers)
    }

    @Test
    fun winner_can_produce_non_null_success_to_downstream() {
        source2.onSuccess(0)

        amb.assertSuccess(0)
    }

    @Test
    fun winner_can_produce_null_success_to_downstream() {
        source2.onSuccess(null)

        amb.assertSuccess(null)
    }

    @Test
    fun winner_can_produce_error_to_downstream() {
        val error = Exception()

        source2.onError(error)

        amb.assertError(error)
    }

    @Test
    fun all_sources_are_disposed_WHEN_first_source_succeeds() {
        source2.onSuccess(0)

        assertFalse(source1.hasSubscribers)
        assertFalse(source2.hasSubscribers)
        assertFalse(source3.hasSubscribers)
    }

    @Test
    fun all_sources_are_disposed_WHEN_first_source_produces_error() {
        source2.onError(Exception())

        assertFalse(source1.hasSubscribers)
        assertFalse(source2.hasSubscribers)
        assertFalse(source3.hasSubscribers)
    }

    @Test
    fun losers_cant_produce_onSuccess_to_downstream() {
        val looserObservers = source1.observers + source3.observers
        source2.onSuccess(0)

        looserObservers.forEach {
            it.onSuccess(1)
        }

        assertNotEquals(1, amb.value)
    }

    @Test
    fun losers_cant_produce_onError_to_downstream() {
        val looserObservers = source1.observers + source3.observers
        source2.onSuccess(0)

        looserObservers.forEach {
            it.onError(Exception())
        }

        amb.assertNotError()
    }

    @Test
    fun error_WHEN_sources_are_empty() {
        val observer = emptyList<Single<Any>>().amb().test()

        observer.assertError { it is NoSuchElementException }
    }
}
