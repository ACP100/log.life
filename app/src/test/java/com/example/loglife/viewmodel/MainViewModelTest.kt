package com.example.loglife.viewmodel

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventInstanceRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainViewModelTest {

    private fun row(id: Long, mode: EventMode, end: Long? = null) = EventInstanceWithType(
        EventInstance(id = id, eventTypeId = id, startTime = 1_000, endTime = end),
        EventType(id = id, name = "Event $id", icon = "", eventMode = mode)
    )

    @Test
    fun `active events are running sessions only`() {
        val repo = mock(EventInstanceRepository::class.java)
        // Instant events also store endTime == null; they must not count as running.
        `when`(repo.getActiveDurationEventsFlow()).thenReturn(
            flowOf(listOf(row(1, EventMode.DURATION), row(2, EventMode.INSTANT), row(3, EventMode.DURATION, end = 2_000)))
        )
        val viewModel = MainViewModel(repo)

        // Collected on Dispatchers.IO; give it a moment.
        val deadline = System.currentTimeMillis() + 2_000
        while (viewModel.activeEvents.value.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(10)

        assertThat(viewModel.activeEvents.value.map { it.eventInstance.id }).containsExactly(1L)
    }
}
