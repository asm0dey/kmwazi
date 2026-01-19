package com.github.asm0dey.kmwazi.domain

import java.security.SecureRandom

/**
 * Production implementation of RandomProvider using SecureRandom for cryptographically secure randomness.
 */
class SecureRandomProvider : RandomProvider {
    private val rng = SecureRandom()

    override fun nextInt(bound: Int): Int = rng.nextInt(bound)

    override fun <T> shuffle(list: List<T>): List<T> {
        val mutable = list.toMutableList()
        // Fisher-Yates shuffle
        for (i in mutable.indices.reversed()) {
            val j = nextInt(i + 1)
            val temp = mutable[i]
            mutable[i] = mutable[j]
            mutable[j] = temp
        }
        return mutable
    }
}
