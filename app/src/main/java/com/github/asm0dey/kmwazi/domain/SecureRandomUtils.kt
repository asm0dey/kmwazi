package com.github.asm0dey.kmwazi.domain

import java.security.SecureRandom

object SecureRandomUtils {
    private val rng = SecureRandom()

    fun chooseOne(ids: List<Long>): Long {
        require(ids.isNotEmpty()) { "ids must not be empty" }
        val idx = rng.nextInt(ids.size)
        return ids[idx]
    }

    fun splitIntoGroups(ids: List<Long>, groupSize: Int): List<List<Long>> {
        require(groupSize > 0) { "groupSize must be > 0" }
        if (ids.isEmpty()) return emptyList()
        val shuffled = ids.shuffled(rng)
        return shuffled.chunked(groupSize)
    }

    fun defineOrder(ids: List<Long>): List<Long> {
        if (ids.isEmpty()) return emptyList()
        return ids.shuffled(rng)
    }
}