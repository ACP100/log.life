package com.example.loglife.ui.log

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.dao.EventTypeStats
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class LogSheetStateTest {

    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 10, 18)
    private fun at(hhmm: String) = day.atTime(LocalTime.parse(hhmm)).toInstant(zone).toEpochMilli()

    private val wokeUp = type(1, "Woke up", null, session = false, pinned = true)
    private val coffee = type(2, "Coffee", "Food", session = false, pinned = true)
    private val deepWork = type(3, "Deep work", "Work", session = true)
    private val lunch = type(4, "Lunch", "Food", session = true)
    private val reading = type(5, "Reading", null, session = true)
    private val yoga = type(6, "Yoga class", "Health", session = true, archived = true)
    private val types = listOf(wokeUp, coffee, deepWork, lunch, reading, yoga)

    private fun type(id: Long, name: String, folder: String?, session: Boolean, pinned: Boolean = false, archived: Boolean = false) =
        EventType(
            id = id,
            name = name,
            icon = "event",
            eventMode = if (session) EventMode.DURATION else EventMode.INSTANT,
            category = folder,
            pinned = pinned,
            archived = archived
        )

    private fun row(id: Long, type: EventType, start: String, end: String? = null) = EventInstanceWithType(
        EventInstance(id = id, eventTypeId = type.id, startTime = at(start), endTime = end?.let(::at)),
        type
    )

    private fun build(query: String = "", stats: List<EventTypeStats> = emptyList()): LogSheetState {
        val running = row(10, deepWork, "11:42")
        val today = listOf(row(11, coffee, "07:05"), row(12, lunch, "12:05", "12:40"), running)
        return buildLogSheetState(types, stats, listOf(running), today, query, zone)
    }

    @Test
    fun `splits pinned, folders and loose events and hides archived`() {
        val state = build()

        assertThat(state.pinned.map { it.name }).containsExactly("Woke up", "Coffee")
        assertThat(state.folders.map { it.name }).containsExactly("Food", "Work").inOrder()
        assertThat(state.folders.first().events.map { it.name }).containsExactly("Coffee", "Lunch").inOrder()
        assertThat(state.loose.map { it.name }).containsExactly("Reading", "Woke up").inOrder()
        assertThat(state.allEvents.map { it.name }).doesNotContain("Yoga class")
    }

    @Test
    fun `marks running sessions and labels today's logs`() {
        val state = build()
        val tiles = state.allEvents.associateBy { it.name }

        assertThat(state.running.single().name).isEqualTo("Deep work")
        assertThat(tiles.getValue("Deep work").runningInstanceId).isEqualTo(10L)
        assertThat(tiles.getValue("Coffee").todayLabel).isEqualTo("07:05")
        assertThat(tiles.getValue("Lunch").todayLabel).isEqualTo("12:05 → 12:40")
        assertThat(tiles.getValue("Reading").todayLabel).isNull()
    }

    @Test
    fun `frequent ranks unpinned events by recent count`() {
        val stats = listOf(
            EventTypeStats(eventTypeId = 2, total = 50, recent = 40, lastStart = 0),
            EventTypeStats(eventTypeId = 5, total = 9, recent = 3, lastStart = 0),
            EventTypeStats(eventTypeId = 4, total = 30, recent = 12, lastStart = 0),
            EventTypeStats(eventTypeId = 3, total = 4, recent = 0, lastStart = 0)
        )

        assertThat(build(stats = stats).frequent.map { it.name }).containsExactly("Lunch", "Reading").inOrder()
    }

    @Test
    fun `search matches names and folders and offers create when no exact match`() {
        val search = build(query = "food").search!!

        assertThat(search.folders.map { it.name }).containsExactly("Food")
        assertThat(search.events.map { it.name }).containsExactly("Coffee", "Lunch")
        assertThat(search.exactMatch).isFalse()
    }

    @Test
    fun `search finds archived events with history`() {
        val stats = listOf(EventTypeStats(eventTypeId = 6, total = 23, recent = 0, lastStart = at("09:00")))
        val search = build(query = "yoga", stats = stats).search!!

        assertThat(search.isEmpty).isTrue()
        assertThat(search.archived.single().total).isEqualTo(23)
        assertThat(search.exactMatch).isFalse()
        assertThat(build(query = "Yoga class").search!!.exactMatch).isTrue()
    }

    @Test
    fun `fits label names the neighbouring logs`() {
        val day = listOf(row(1, coffee, "08:50"), row(2, reading, "10:30", "11:20"))

        assertThat(fitsLabel(day, at("09:10"), at("10:15"), zone)).isEqualTo("Fits between Coffee 08:50 and Reading 10:30")
        assertThat(fitsLabel(day, at("12:00"), at("12:30"), zone)).isEqualTo("After Reading 10:30")
        assertThat(fitsLabel(emptyList(), at("12:00"), at("12:30"), zone)).isEqualTo("Nothing else logged that day")
    }
}
