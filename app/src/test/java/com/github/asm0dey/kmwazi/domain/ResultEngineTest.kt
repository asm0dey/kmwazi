package com.github.asm0dey.kmwazi.domain

import com.github.asm0dey.kmwazi.FakeRandomProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultEngineTest {

    @Test
    fun `chooseOne returns first element with fake random returning 0`() {
        val fakeRandom = FakeRandomProvider(fixedInts = listOf(0))
        val engine = ResultEngine(fakeRandom)

        val result = engine.chooseOne(listOf(10L, 20L, 30L))

        assertEquals(10L, result)
    }

    @Test
    fun `chooseOne returns second element with fake random returning 1`() {
        val fakeRandom = FakeRandomProvider(fixedInts = listOf(1))
        val engine = ResultEngine(fakeRandom)

        val result = engine.chooseOne(listOf(10L, 20L, 30L))

        assertEquals(20L, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `chooseOne throws on empty list`() {
        val fakeRandom = FakeRandomProvider()
        val engine = ResultEngine(fakeRandom)

        engine.chooseOne(emptyList())
    }

    @Test
    fun `splitIntoGroups with even division`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = false)
        val engine = ResultEngine(fakeRandom)

        val result = engine.splitIntoGroups(listOf(1L, 2L, 3L, 4L), groupSize = 2)

        assertEquals(2, result.size)
        assertEquals(listOf(1L, 2L), result[0])
        assertEquals(listOf(3L, 4L), result[1])
    }

    @Test
    fun `splitIntoGroups with remainder`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = false)
        val engine = ResultEngine(fakeRandom)

        val result = engine.splitIntoGroups(listOf(1L, 2L, 3L, 4L, 5L), groupSize = 2)

        assertEquals(3, result.size)
        assertEquals(listOf(1L, 2L), result[0])
        assertEquals(listOf(3L, 4L), result[1])
        assertEquals(listOf(5L), result[2])
    }

    @Test
    fun `splitIntoGroups shuffles when random provider shuffles`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = true)
        val engine = ResultEngine(fakeRandom)

        val result = engine.splitIntoGroups(listOf(1L, 2L, 3L, 4L), groupSize = 2)

        // FakeRandomProvider with shouldShuffle=true reverses the list
        assertEquals(2, result.size)
        assertEquals(listOf(4L, 3L), result[0])
        assertEquals(listOf(2L, 1L), result[1])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splitIntoGroups throws on empty list`() {
        val fakeRandom = FakeRandomProvider()
        val engine = ResultEngine(fakeRandom)

        engine.splitIntoGroups(emptyList(), groupSize = 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splitIntoGroups throws on zero groupSize`() {
        val fakeRandom = FakeRandomProvider()
        val engine = ResultEngine(fakeRandom)

        engine.splitIntoGroups(listOf(1L, 2L), groupSize = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splitIntoGroups throws on negative groupSize`() {
        val fakeRandom = FakeRandomProvider()
        val engine = ResultEngine(fakeRandom)

        engine.splitIntoGroups(listOf(1L, 2L), groupSize = -1)
    }

    @Test
    fun `defineOrder returns shuffled list`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = true)
        val engine = ResultEngine(fakeRandom)

        val result = engine.defineOrder(listOf(1L, 2L, 3L))

        // FakeRandomProvider with shouldShuffle=true reverses the list
        assertEquals(listOf(3L, 2L, 1L), result)
    }

    @Test
    fun `defineOrder returns original order when not shuffling`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = false)
        val engine = ResultEngine(fakeRandom)

        val result = engine.defineOrder(listOf(1L, 2L, 3L))

        assertEquals(listOf(1L, 2L, 3L), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `defineOrder throws on empty list`() {
        val fakeRandom = FakeRandomProvider()
        val engine = ResultEngine(fakeRandom)

        engine.defineOrder(emptyList())
    }

    @Test
    fun `chooseOne with single element returns that element`() {
        val fakeRandom = FakeRandomProvider(fixedInts = listOf(0))
        val engine = ResultEngine(fakeRandom)

        val result = engine.chooseOne(listOf(42L))

        assertEquals(42L, result)
    }

    @Test
    fun `splitIntoGroups with groupSize equal to list size creates one group`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = false)
        val engine = ResultEngine(fakeRandom)

        val result = engine.splitIntoGroups(listOf(1L, 2L, 3L), groupSize = 3)

        assertEquals(1, result.size)
        assertEquals(listOf(1L, 2L, 3L), result[0])
    }

    @Test
    fun `splitIntoGroups with groupSize larger than list creates one group`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = false)
        val engine = ResultEngine(fakeRandom)

        val result = engine.splitIntoGroups(listOf(1L, 2L), groupSize = 5)

        assertEquals(1, result.size)
        assertEquals(listOf(1L, 2L), result[0])
    }

    @Test
    fun `splitIntoGroups with groupSize 1 creates individual groups`() {
        val fakeRandom = FakeRandomProvider(shouldShuffle = false)
        val engine = ResultEngine(fakeRandom)

        val result = engine.splitIntoGroups(listOf(1L, 2L, 3L), groupSize = 1)

        assertEquals(3, result.size)
        assertEquals(listOf(1L), result[0])
        assertEquals(listOf(2L), result[1])
        assertEquals(listOf(3L), result[2])
    }

    @Test
    fun `SecureRandomProvider integration test chooses from list`() {
        // Integration test with actual SecureRandomProvider
        val secureRandom = SecureRandomProvider()
        val engine = ResultEngine(secureRandom)

        val ids = listOf(1L, 2L, 3L, 4L, 5L)
        val result = engine.chooseOne(ids)

        assertTrue("Result should be in the input list", result in ids)
    }

    @Test
    fun `SecureRandomProvider integration test splits into groups`() {
        val secureRandom = SecureRandomProvider()
        val engine = ResultEngine(secureRandom)

        val ids = listOf(1L, 2L, 3L, 4L, 5L, 6L)
        val result = engine.splitIntoGroups(ids, groupSize = 2)

        assertEquals(3, result.size)
        val allIds = result.flatten()
        assertEquals(ids.size, allIds.size)
        assertTrue("All IDs should be present", allIds.toSet() == ids.toSet())
    }

    @Test
    fun `SecureRandomProvider integration test defines order`() {
        val secureRandom = SecureRandomProvider()
        val engine = ResultEngine(secureRandom)

        val ids = listOf(1L, 2L, 3L, 4L, 5L)
        val result = engine.defineOrder(ids)

        assertEquals(ids.size, result.size)
        assertTrue("All IDs should be present", result.toSet() == ids.toSet())
    }
}
