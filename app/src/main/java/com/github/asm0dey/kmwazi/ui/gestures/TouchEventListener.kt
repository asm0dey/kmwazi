package com.github.asm0dey.kmwazi.ui.gestures

import androidx.compose.ui.geometry.Offset

/**
 * Interface for high-level touch events.
 */
interface TouchEventListener {
    fun onFingerDown(id: Long, position: Offset)
    fun onFingerMove(id: Long, position: Offset)
    fun onFingerUp(id: Long)
    fun onAllFingersUp()
    fun onLongPress()
}
