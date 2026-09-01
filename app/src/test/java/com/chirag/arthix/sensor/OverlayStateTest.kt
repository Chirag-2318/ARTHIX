package com.chirag.arthix.sensor

import com.chirag.arthix.ui.overlay.DEFAULT_OVERLAY_CATEGORIES
import com.chirag.arthix.ui.overlay.OverlayDisplayState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for overlay states and default category configurations.
 */
class OverlayStateTest {

    @Test
    fun `overlay display state values exist`() {
        assertThat(OverlayDisplayState.EXPANDED).isNotNull()
        assertThat(OverlayDisplayState.COLLAPSED).isNotNull()
    }

    @Test
    fun `default overlay categories contain FR1 categories`() {
        val categoryNames = DEFAULT_OVERLAY_CATEGORIES.map { it.name }
        assertThat(categoryNames).containsExactly("Food", "Travel", "Shopping", "Other").inOrder()
    }
}
