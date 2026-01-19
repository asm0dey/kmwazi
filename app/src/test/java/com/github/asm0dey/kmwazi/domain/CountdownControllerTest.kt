package com.github.asm0dey.kmwazi.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownControllerTest {

    @Test
    fun `countdown starts with initial duration`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)

        controller.start(300L) { }
        runCurrent()

        assertEquals(300L, controller.remainingMs.value)
    }

    @Test
    fun `countdown decrements over time`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)

        controller.start(300L) { }

        advanceTimeBy(100L)
        runCurrent()
        assertEquals(200L, controller.remainingMs.value)

        advanceTimeBy(100L)
        runCurrent()
        assertEquals(100L, controller.remainingMs.value)
    }

    @Test
    fun `countdown expires and calls callback`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)
        var expired = false

        controller.start(300L) { expired = true }

        advanceTimeBy(100L)
        runCurrent()
        assertFalse("Should not expire yet", expired)

        advanceTimeBy(200L)
        runCurrent()
        assertEquals(0L, controller.remainingMs.value)
        assertTrue("Should expire after full duration", expired)
    }

    @Test
    fun `cancel stops countdown`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)
        var expired = false

        controller.start(300L) { expired = true }
        advanceTimeBy(100L)
        runCurrent()
        assertEquals(200L, controller.remainingMs.value)

        controller.cancel()

        assertNull("Remaining should be null after cancel", controller.remainingMs.value)
        assertFalse("Should not be active after cancel", controller.isActive())

        advanceTimeBy(300L)
        runCurrent()
        assertFalse("Should not expire after cancel", expired)
    }

    @Test
    fun `restart cancels previous countdown`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)
        var firstExpired = false
        var secondExpired = false

        controller.start(300L) { firstExpired = true }
        advanceTimeBy(100L)
        runCurrent()
        assertEquals(200L, controller.remainingMs.value)

        controller.start(400L) { secondExpired = true }
        runCurrent()
        assertEquals(400L, controller.remainingMs.value)

        advanceTimeBy(500L)
        runCurrent()
        assertFalse("First countdown should not expire", firstExpired)
        assertTrue("Second countdown should expire", secondExpired)
    }

    @Test
    fun `isActive returns true during countdown`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)

        assertFalse("Should not be active initially", controller.isActive())

        controller.start(300L) { }
        assertTrue("Should be active after start", controller.isActive())

        controller.cancel()
        assertFalse("Should not be active after cancel", controller.isActive())
    }

    @Test
    fun `countdown with zero duration expires immediately`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)
        var expired = false

        controller.start(0L) { expired = true }

        advanceTimeBy(100L)
        runCurrent()
        assertEquals(0L, controller.remainingMs.value)
        assertTrue("Should expire immediately with 0ms duration", expired)
    }

    @Test
    fun `multiple cancel calls are safe`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)

        controller.cancel()
        controller.cancel()
        controller.cancel()

        assertNull("Remaining should be null", controller.remainingMs.value)
        assertFalse("Should not be active", controller.isActive())
    }

    @Test
    fun `countdown does not go below zero`() = runTest {
        val controller = CountdownController(this, tickMs = 100L)

        controller.start(150L) { }

        advanceTimeBy(100L)
        runCurrent()
        assertEquals(50L, controller.remainingMs.value)

        advanceTimeBy(100L)
        runCurrent()
        assertEquals(0L, controller.remainingMs.value)

        advanceTimeBy(100L)
        runCurrent()
        assertEquals(0L, controller.remainingMs.value)
    }
}
