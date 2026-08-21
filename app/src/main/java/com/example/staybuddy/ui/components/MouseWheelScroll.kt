package com.example.staybuddy.ui.components

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter

private const val MouseWheelScrollMultiplier = 96f

fun Modifier.mouseWheelScroll(scrollState: ScrollState): Modifier =
    mouseWheelScroll { delta -> scrollState.dispatchRawDelta(delta) }

fun Modifier.mouseWheelScroll(listState: LazyListState): Modifier =
    mouseWheelScroll { delta -> listState.dispatchRawDelta(delta) }

private fun Modifier.mouseWheelScroll(scrollBy: (Float) -> Float): Modifier =
    pointerInteropFilter { event ->
        if (event.actionMasked == MotionEvent.ACTION_SCROLL && event.isMouseWheelEvent()) {
            val verticalDelta = -event.getAxisValue(MotionEvent.AXIS_VSCROLL) * MouseWheelScrollMultiplier
            val consumed = scrollBy(verticalDelta)
            consumed != 0f
        } else {
            false
        }
    }

private fun MotionEvent.isMouseWheelEvent(): Boolean =
    (source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE ||
        (source and InputDevice.SOURCE_ROTARY_ENCODER) == InputDevice.SOURCE_ROTARY_ENCODER ||
        (source and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD
