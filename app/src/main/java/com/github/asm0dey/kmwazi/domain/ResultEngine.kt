package com.github.asm0dey.kmwazi.domain

/**
 * Pure domain service for computing game results using the Mode pattern.
 * All randomization is delegated to the injected RandomProvider.
 */
class ResultEngine(private val randomProvider: RandomProvider) {

    /**
     * Chooses one ID randomly from the list.
     * @throws IllegalArgumentException if ids is empty
     */
    fun chooseOne(ids: List<Long>): Long {
        require(ids.isNotEmpty()) { "Cannot choose from empty list" }
        val index = randomProvider.nextInt(ids.size)
        return ids[index]
    }

    /**
     * Splits IDs into groups of the specified size.
     * The last group may be smaller if the total doesn't divide evenly.
     * @throws IllegalArgumentException if ids is empty or groupSize <= 0
     */
    fun splitIntoGroups(ids: List<Long>, groupSize: Int): List<List<Long>> {
        require(ids.isNotEmpty()) { "Cannot split empty list" }
        require(groupSize > 0) { "Group size must be positive" }

        val shuffled = randomProvider.shuffle(ids)
        return shuffled.chunked(groupSize)
    }

    /**
     * Returns a shuffled order of all IDs.
     * @throws IllegalArgumentException if ids is empty
     */
    fun defineOrder(ids: List<Long>): List<Long> {
        require(ids.isNotEmpty()) { "Cannot order empty list" }
        return randomProvider.shuffle(ids)
    }
}
