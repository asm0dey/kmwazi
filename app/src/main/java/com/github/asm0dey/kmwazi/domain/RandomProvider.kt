package com.github.asm0dey.kmwazi.domain

/**
 * Interface for providing randomness, abstracted to allow deterministic testing.
 */
interface RandomProvider {
    /**
     * Returns a pseudo-random integer between 0 (inclusive) and [bound] (exclusive).
     * @param bound the upper bound (exclusive). Must be positive.
     */
    fun nextInt(bound: Int): Int

    /**
     * Returns a shuffled copy of the list using this RandomProvider's randomness.
     */
    fun <T> shuffle(list: List<T>): List<T>
}
