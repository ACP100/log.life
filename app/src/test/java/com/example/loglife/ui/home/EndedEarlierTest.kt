package com.example.loglife.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class EndedEarlierTest {

    private val zone = ZoneOffset.UTC
    private fun at(day: Int, h: Int, m: Int) = LocalDateTime.of(2026, 10, day, h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `picked time later the same day`() {
        assertThat(endFromPickedTime(at(18, 9, 15), LocalTime.of(11, 0), now = at(18, 13, 19), zone)).isEqualTo(at(18, 11, 0))
    }

    @Test
    fun `a time before the start means the next morning`() {
        assertThat(endFromPickedTime(at(17, 22, 0), LocalTime.of(1, 30), now = at(18, 9, 0), zone)).isEqualTo(at(18, 1, 30))
    }

    @Test
    fun `future times are not allowed`() {
        assertThat(endFromPickedTime(at(18, 9, 15), LocalTime.of(14, 0), now = at(18, 13, 19), zone)).isNull()
    }
}
