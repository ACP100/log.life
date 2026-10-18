package com.example.loglife.ui.settings

import com.example.loglife.data.dao.EventTypeStats
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EventsStateTest {

    private fun t(id: Long, name: String, category: String?, order: Int, archived: Boolean = false, updatedAt: Long = 0) =
        EventType(id = id, name = name, icon = "", eventMode = EventMode.INSTANT, category = category, widgetOrder = order, archived = archived, updatedAt = updatedAt)

    @Test
    fun `categories follow the user's order, uncategorised last, archived apart`() {
        val state = buildEventsState(
            types = listOf(
                t(1, "Lunch", "Food", order = 5),
                t(2, "Deep work", "Work", order = 0),
                t(3, "Coffee", "Food", order = 1),
                t(4, "Stretch", null, order = 2),
                t(5, "Green tea", "Food", order = 3, archived = true, updatedAt = 10),
                t(6, "Running", "Health", order = 4, archived = true, updatedAt = 20),
            ),
            stats = listOf(EventTypeStats(eventTypeId = 3, total = 214, recent = 0, lastStart = 0))
        )
        assertThat(state.groups.map { it.name }).containsExactly("Work", "Food", null).inOrder()
        assertThat(state.groups[1].events.map { it.type.name }).containsExactly("Coffee", "Lunch").inOrder()
        assertThat(state.groups[1].events.first().logCount).isEqualTo(214)
        assertThat(state.archived.map { it.type.name }).containsExactly("Running", "Green tea").inOrder()
        assertThat(state.categoryNames).containsExactly("Work", "Food").inOrder()
    }

    @Test
    fun `blank categories count as none`() {
        val state = buildEventsState(listOf(t(1, "A", "  ", 0)), emptyList())
        assertThat(state.groups.single().name).isNull()
    }
}
