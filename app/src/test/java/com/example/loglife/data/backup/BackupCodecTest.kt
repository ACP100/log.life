package com.example.loglife.data.backup

import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupCodecTest {

    private val coffee = EventType(id = 1, name = "Coffee", icon = "event", eventMode = EventMode.INSTANT, category = "Food", color = "#C98A1E", createdAt = 5, updatedAt = 6)
    private val work = EventType(id = 2, name = "Deep work", icon = "event", eventMode = EventMode.DURATION, category = null, archived = true, createdAt = 7, updatedAt = 8)
    private val data = BackupData(
        eventTypes = listOf(coffee, work),
        logs = listOf(
            EventInstance(id = 10, eventTypeId = 1, startTime = 1_000, note = "with \"oat\", milk", createdAt = 1, updatedAt = 2),
            EventInstance(id = 11, eventTypeId = 2, startTime = 60_000, endTime = 60_000 + 65 * 60_000, createdAt = 3, updatedAt = 4),
            EventInstance(id = 12, eventTypeId = 2, startTime = 9_000_000, endTime = null, createdAt = 5, updatedAt = 5),
        )
    )

    @Test
    fun `json backup round-trips every field`() {
        val back = BackupCodec.fromJson(BackupCodec.toJson(data, exportedAt = 42))
        assertThat(back).isEqualTo(data)
    }

    @Test
    fun `logs of unknown event types are dropped on restore`() {
        val orphan = data.copy(logs = data.logs + EventInstance(id = 99, eventTypeId = 777, startTime = 1))
        val back = BackupCodec.fromJson(BackupCodec.toJson(orphan, exportedAt = 42))
        assertThat(back.logs.map { it.id }).doesNotContain(99L)
    }

    @Test
    fun `anything that is not a backup is rejected`() {
        assertThrows(BackupFormatException::class.java) { BackupCodec.fromJson("not json") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.fromJson("""{"format":"other"}""") }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.fromJson("""{"format":"${BackupCodec.FORMAT}","version":1,"eventTypes":[{"id":1}],"logs":[]}""")
        }
    }

    @Test
    fun `newer backups are refused`() {
        val newer = BackupCodec.toJson(data, 42).replace("\"version\": ${BackupCodec.VERSION}", "\"version\": 99")
        assertThrows(BackupFormatException::class.java) { BackupCodec.fromJson(newer) }
    }

    @Test
    fun `csv has one row per log with quoted fields and session minutes`() {
        val lines = BackupCodec.toCsv(data, ZoneOffset.UTC).trimEnd().lines()
        assertThat(lines[0]).isEqualTo("event,category,type,start,end,duration_minutes,note")
        assertThat(lines).hasSize(4)
        assertThat(lines[1]).isEqualTo("Coffee,Food,moment,1970-01-01T00:00:01Z,,,\"with \"\"oat\"\", milk\"")
        assertThat(lines[2]).isEqualTo("Deep work,,session,1970-01-01T00:01:00Z,1970-01-01T01:06:00Z,65,")
        // Still running: no end, no duration.
        assertThat(lines[3]).startsWith("Deep work,,session,1970-01-01T02:30:00Z,,,")
    }
}
