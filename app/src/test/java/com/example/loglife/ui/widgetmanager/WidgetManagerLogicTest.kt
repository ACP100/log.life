package com.example.loglife.ui.widgetmanager

import com.example.loglife.data.settings.WidgetSettings
import com.example.loglife.ui.widgets.PlacedWidget
import com.example.loglife.ui.widgets.WidgetKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetManagerLogicTest {

    @Test
    fun `moving a favourite shifts the others`() {
        val list = listOf("a", "b", "c", "d")
        assertThat(list.moved(0, 2)).containsExactly("b", "c", "a", "d").inOrder()
        assertThat(list.moved(3, 1)).containsExactly("a", "d", "b", "c").inOrder()
        assertThat(list.moved(1, 1)).isSameInstanceAs(list)
        assertThat(list.moved(0, 9)).isSameInstanceAs(list)
    }

    @Test
    fun `placed summaries`() {
        val two = listOf(PlacedWidget(1, 4, 2), PlacedWidget(2, 4, 1))
        assertThat(placedSummary(WidgetKind.DECK, two, WidgetSettings())).isEqualTo("2 placed · 4×2 and 4×1")
        assertThat(placedSummary(WidgetKind.TODAY, listOf(PlacedWidget(3, 4, 2)), WidgetSettings())).isEqualTo("1 placed · 4×2")
        assertThat(placedSummary(WidgetKind.BUTTONS, listOf(PlacedWidget(4, 4, 1)), WidgetSettings(buttonCount = 4))).isEqualTo("1 placed · 4 buttons")
        assertThat(placedSummary(WidgetKind.BUTTONS, emptyList(), WidgetSettings())).isEqualTo("Not on your home screen")
    }

    @Test
    fun `deck size names match the layout buckets`() {
        assertThat(deckSizeName(PlacedWidget(1, 4, 2))).isEqualTo("Large deck")
        assertThat(deckSizeName(PlacedWidget(1, 4, 1))).isEqualTo("Slim deck")
        assertThat(deckSizeName(PlacedWidget(1, 2, 2))).isEqualTo("Small deck")
    }
}
