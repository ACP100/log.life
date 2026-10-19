package com.example.loglife.ui.widgets

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SingleTileTest {

    private val coffee = EventType(id = 1, name = "Coffee", icon = "", eventMode = EventMode.INSTANT, showInWidget = true, widgetOrder = 1)
    private val gym = EventType(id = 2, name = "Gym", icon = "", eventMode = EventMode.DURATION, showInWidget = true, widgetOrder = 0)
    private val read = EventType(id = 3, name = "Reading", icon = "", eventMode = EventMode.DURATION, showInWidget = false)
    private val alpha = EventType(id = 4, name = "Art", icon = "", eventMode = EventMode.INSTANT, showInWidget = false)
    private val old = EventType(id = 5, name = "Old", icon = "", eventMode = EventMode.INSTANT, archived = true)

    @Test
    fun `idle moment carries its last log`() {
        val tile = WidgetData.buildSingle(coffee, emptyList(), lastStart = 1_000L)!!
        assertThat(tile.isRunning).isFalse()
        assertThat(tile.isSession).isFalse()
        assertThat(tile.lastStart).isEqualTo(1_000L)
    }

    @Test
    fun `running session shows its instance`() {
        val active = listOf(EventInstanceWithType(EventInstance(id = 9, eventTypeId = 2, startTime = 500L), gym))
        val tile = WidgetData.buildSingle(gym, active, lastStart = 500L)!!
        assertThat(tile.isRunning).isTrue()
        assertThat(tile.instanceId).isEqualTo(9)
        assertThat(tile.runningSince).isEqualTo(500L)
    }

    @Test
    fun `missing or archived event needs a new pick`() {
        assertThat(WidgetData.buildSingle(null, emptyList(), null)).isNull()
        assertThat(WidgetData.buildSingle(old, emptyList(), null)).isNull()
    }

    @Test
    fun `picker lists favourites in order, then the rest by name, no archived`() {
        assertThat(pickable(listOf(coffee, read, old, gym, alpha)).map { it.name })
            .containsExactly("Gym", "Coffee", "Art", "Reading").inOrder()
    }
}
