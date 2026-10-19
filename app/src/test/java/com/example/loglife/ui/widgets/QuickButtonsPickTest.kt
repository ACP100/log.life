package com.example.loglife.ui.widgets

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuickButtonsPickTest {

    private val coffee = EventType(id = 1, name = "Coffee", icon = "", eventMode = EventMode.INSTANT, showInWidget = true, widgetOrder = 1)
    private val gym = EventType(id = 2, name = "Gym", icon = "", eventMode = EventMode.DURATION, showInWidget = true, widgetOrder = 0)
    private val read = EventType(id = 3, name = "Reading", icon = "", eventMode = EventMode.DURATION, showInWidget = false)
    private val art = EventType(id = 4, name = "Art", icon = "", eventMode = EventMode.INSTANT, showInWidget = false)
    private val old = EventType(id = 5, name = "Old", icon = "", eventMode = EventMode.INSTANT, archived = true)
    private val all = listOf(coffee, gym, read, art, old)

    @Test
    fun `picked events keep tap order and drop archived or deleted ones`() {
        assertThat(WidgetData.pickedTypes(all, listOf(3, 5, 99, 1)).map { it.name })
            .containsExactly("Reading", "Coffee").inOrder()
    }

    @Test
    fun `a single picked session shows its running state`() {
        val active = listOf(EventInstanceWithType(EventInstance(id = 9, eventTypeId = 3, startTime = 500L), read))
        val buttons = WidgetData.buildButtons(active, WidgetData.pickedTypes(all, listOf(3)), count = 6)
        assertThat(buttons).hasSize(1)
        assertThat(buttons[0].isRunning).isTrue()
        assertThat(buttons[0].instanceId).isEqualTo(9)
    }

    @Test
    fun `toggling adds at the end, removes, and stops at the max`() {
        assertThat(togglePick(listOf(1L), 3L, max = 6)).containsExactly(1L, 3L).inOrder()
        assertThat(togglePick(listOf(1L, 3L), 1L, max = 6)).containsExactly(3L)
        assertThat(togglePick(listOf(1L), 3L, max = 1)).containsExactly(1L)
    }

    @Test
    fun `picker lists favourites in order, then the rest by name, no archived`() {
        assertThat(pickable(all).map { it.name })
            .containsExactly("Gym", "Coffee", "Art", "Reading").inOrder()
    }
}
