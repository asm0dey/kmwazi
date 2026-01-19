package com.github.asm0dey.kmwazi

import com.github.asm0dey.kmwazi.domain.RandomProvider

/**
 * Deterministic RandomProvider for testing.
 *
 * @param fixedInts List of integers to return from nextInt() calls in sequence.
 *                  Cycles through the list if more calls are made than elements.
 * @param shouldShuffle If true, shuffle() will reverse the list. If false, returns input unchanged.
 */
class FakeRandomProvider(
    private val fixedInts: List<Int> = listOf(0),
    private val shouldShuffle: Boolean = false,
) : RandomProvider {
    private var intIndex = 0

    override fun nextInt(bound: Int): Int {
        val value = fixedInts[intIndex % fixedInts.size]
        intIndex++
        return value
    }

    override fun <T> shuffle(list: List<T>): List<T> {
        return if (shouldShuffle) list.reversed() else list
    }
}
