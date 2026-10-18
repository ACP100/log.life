package com.example.loglife.ui.widgets

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WidgetDataTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private fun at(day: Int, h: Int, m: Int) =
        LocalDateTime.of(2026, 10, day, h, m).atZone(zone).toInstant().toEpochMilli()

    private val coffee = EventType(id = 1, name = "Coffee", icon = "", eventMode = EventMode.INSTANT, category = "Food", widgetOrder = 0)
    private val gym = EventType(id = 2, name = "Gym", icon = "", eventMode = EventMode.DURATION, category = "Health", widgetOrder = 1)
    private val work = EventType(id = 3, name = "Deep work", icon = "", eventMode = EventMode.DURATION, category = "Work", widgetOrder = 2)
    private val old = EventType(id = 4, name = "Old", icon = "", eventMode = EventMode.INSTANT, archived = true)

    private fun running(id: Long, type: EventType, start: Long) =
        EventInstanceWithType(EventInstance(id = id, eventTypeId = type.id, startTime = start), type)

    @Test
    fun `deck puts running sessions first and does not repeat them as favourites`() {
        val deck = WidgetData.buildDeck(
            active = listOf(running(10, work, at(18, 9, 0))),
            favourites = listOf(coffee, gym, work, old),
            lastStarts = mapOf(1L to at(18, 7, 40)),
        )
        assertThat(deck.map { it.name }).containsExactly("Deep work", "Coffee", "Gym").inOrder()
        assertThat(deck[0].isRunning).isTrue()
        assertThat(deck[1].lastStart).isEqualTo(at(18, 7, 40))
        assertThat(deck[1].isSession).isFalse()
    }

    @Test
    fun `instant events with null endTime are not treated as running`() {
        val deck = WidgetData.buildDeck(
            active = listOf(running(11, coffee, at(18, 8, 0))),
            favourites = listOf(coffee),
            lastStarts = emptyMap(),
        )
        assertThat(deck).hasSize(1)
        assertThat(deck[0].isRunning).isFalse()
    }

    @Test
    fun `empty deck when no favourites and nothing running`() {
        assertThat(WidgetData.buildDeck(emptyList(), emptyList(), emptyMap())).isEmpty()
    }

    @Test
    fun `deck jumps to a session that just started`() {
        val deck = WidgetData.buildDeck(
            active = listOf(running(10, work, at(18, 9, 0)), running(12, gym, at(18, 10, 0))),
            favourites = listOf(coffee, gym, work),
            lastStarts = emptyMap(),
        )
        val index = WidgetData.pickDeckIndex(deck, prevIndex = 2, prevTypeId = 1, prevActive = setOf(10))
        assertThat(deck[index].name).isEqualTo("Gym")
    }

    @Test
    fun `deck keeps the same type in view after it stops`() {
        val deck = WidgetData.buildDeck(emptyList(), listOf(coffee, gym, work), emptyMap())
        val index = WidgetData.pickDeckIndex(deck, prevIndex = 0, prevTypeId = 3, prevActive = setOf(10))
        assertThat(deck[index].name).isEqualTo("Deep work")
    }

    @Test
    fun `stepping wraps around`() {
        val deck = WidgetData.buildDeck(emptyList(), listOf(coffee, gym, work), emptyMap())
        assertThat(WidgetData.pickDeckIndex(deck, prevIndex = -1, prevTypeId = -1, prevActive = emptySet())).isEqualTo(2)
        assertThat(WidgetData.pickDeckIndex(deck, prevIndex = 3, prevTypeId = -1, prevActive = emptySet())).isEqualTo(0)
    }

    @Test
    fun `buttons keep favourite order and show running state`() {
        val buttons = WidgetData.buildButtons(
            active = listOf(running(10, gym, at(18, 9, 0))),
            favourites = listOf(coffee, gym, work),
            count = 2,
        )
        assertThat(buttons.map { it.name }).containsExactly("Coffee", "Gym").inOrder()
        assertThat(buttons[1].runningSince).isEqualTo(at(18, 9, 0))
        assertThat(buttons[1].instanceId).isEqualTo(10)
    }

    @Test
    fun `today splits running from logged and totals finished sessions`() {
        val rows = listOf(
            EventInstanceWithType(EventInstance(id = 1, eventTypeId = 1, startTime = at(18, 8, 50)), coffee),
            EventInstanceWithType(EventInstance(id = 2, eventTypeId = 2, startTime = at(18, 10, 30), endTime = at(18, 11, 20)), gym),
            running(3, work, at(18, 12, 0)),
        )
        val today = WidgetData.buildToday(rows)
        assertThat(today.running.map { it.name }).containsExactly("Deep work")
        assertThat(today.logged.map { it.name }).containsExactly("Gym", "Coffee").inOrder()
        assertThat(today.count).isEqualTo(3)
        assertThat(WidgetData.formatTotal(today.totalLoggedMillis)).isEqualTo("50m")
    }

    @Test
    fun `last formatting by age`() {
        val now = at(18, 14, 12)
        assertThat(WidgetData.formatLast(at(18, 7, 40), now, zone)).isEqualTo("last 07:40")
        assertThat(WidgetData.formatLast(at(17, 22, 10), now, zone)).isEqualTo("last yest 22:10")
        assertThat(WidgetData.formatLast(at(14, 8, 0), now, zone)).startsWith("last ")
        assertThat(WidgetData.formatLast(at(14, 8, 0), now, zone)).endsWith("08:00")
        assertThat(WidgetData.formatLast(null, now, zone)).isEqualTo("not logged yet")
    }

    @Test
    fun `total formats hours with padded minutes`() {
        assertThat(WidgetData.formatTotal((5 * 60 + 1) * 60_000L)).isEqualTo("5h 01m")
        assertThat(WidgetData.formatTotal(0)).isEqualTo("0m")
    }

    @Test
    fun `blend mixes towards the background`() {
        val c = WidgetStyle.blend(0xFFFF0000.toInt(), 0xFF000000.toInt(), 0.5f)
        assertThat((c shr 16) and 0xFF).isIn(127..128)
        assertThat(c ushr 24).isEqualTo(0xFF)
    }
}
