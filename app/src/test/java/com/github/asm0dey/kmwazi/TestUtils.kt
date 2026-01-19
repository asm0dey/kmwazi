package com.github.asm0dey.kmwazi

import androidx.compose.ui.geometry.Offset

/**
 * Test utilities for Kmwazi tests.
 */
object TestUtils {
    /**
     * Creates a list of finger IDs for testing.
     */
    fun createFingerIds(count: Int): List<Long> {
        return (1L..count.toLong()).toList()
    }

    /**
     * Creates a map of finger IDs to positions for testing.
     */
    fun createTouchMap(count: Int): Map<Long, Offset> {
        return createFingerIds(count).associateWith { id ->
            Offset(id * 100f, id * 100f)
        }
    }
}
