package com.example.loglife.ui.widgets

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The per-widget options from Settings → Widgets. */
class WidgetOptionsTest {

    private val coffee = EventType(id = 1, name = "Coffee", icon = "", eventMode = EventMode.INSTANT, widgetOrder = 0)
    private val gym = EventType(id = 2, name = "Gym", icon = "", eventMode = EventMode.DURATION, widgetOrder = 1)
    private val work = EventType(id = 3, name = "Deep work", icon = "", eventMode = EventMode.DURATION, widgetOrder = 2)
    private val laundry = EventType(id = 4, name = "Laundry", icon = "", eventMode = EventMode.DURATION, showInWidget = false)

    private fun running(id: Long, type: EventType, start: Long) =
        EventInstanceWithType(EventInstance(id = id, eventTypeId = type.id, startTime = start), type)

    private val active = listOf(running(10, work, 100), running(11, laundry, 200))

    @Test
    fun `deck without running-first keeps favourites in place and appends other sessions`() {
        val deck = WidgetData.buildDeck(active, listOf(coffee, gym, work), emptyMap(), runningFirst = false)
        assertThat(deck.map { it.name }).containsExactly("Coffee", "Gym", "Deep work", "Laundry").inOrder()
        assertThat(deck[2].isRunning).isTrue()
        assertThat(deck[3].isRunning).isTrue()
    }

    @Test
    fun `deck with running-first leads with every running session`() {
        val deck = WidgetData.buildDeck(active, listOf(coffee, gym, work), emptyMap(), runningFirst = true)
        assertThat(deck.map { it.name }).containsExactly("Deep work", "Laundry", "Coffee", "Gym").inOrder()
    }

    @Test
    fun `buttons keep their places by default`() {
        val buttons = WidgetData.buildButtons(active, listOf(coffee, gym, work), count = 2)
        assertThat(buttons.map { it.name }).containsExactly("Coffee", "Gym").inOrder()
    }

    @Test
    fun `buttons with running-first show running sessions first`() {
        val buttons = WidgetData.buildButtons(active, listOf(coffee, gym, work), count = 3, runningFirst = true)
        assertThat(buttons.map { it.name }).containsExactly("Deep work", "Laundry", "Coffee").inOrder()
        assertThat(buttons.take(2).all { it.isRunning }).isTrue()
    }

    @Test
    fun `placed widget size in cells`() {
        assertThat(WidgetPlacement.cells(250, default = 4)).isEqualTo(4)
        assertThat(WidgetPlacement.cells(110, default = 2)).isEqualTo(2)
        assertThat(WidgetPlacement.cells(40, default = 2)).isEqualTo(1)
        assertThat(WidgetPlacement.cells(0, default = 2)).isEqualTo(2)
    }
}
