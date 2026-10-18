package com.example.loglife.data.backup

import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Everything a backup holds: event types and their logs. */
data class BackupData(val eventTypes: List<EventType>, val logs: List<EventInstance>)

class BackupFormatException(message: String) : Exception(message)

/**
 * Export / restore formats. JSON is the backup (it round-trips through
 * [fromJson]); CSV is a read-only export of the logs for spreadsheets.
 */
object BackupCodec {

    const val FORMAT = "log.life-backup"
    const val VERSION = 1

    fun toJson(data: BackupData, exportedAt: Long): String {
        val types = JSONArray()
        data.eventTypes.forEach { t ->
            types.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("icon", t.icon)
                    .put("mode", t.eventMode.name)
                    .put("category", t.category ?: JSONObject.NULL)
                    .put("showInWidget", t.showInWidget)
                    .put("widgetOrder", t.widgetOrder)
                    .put("color", t.color ?: JSONObject.NULL)
                    .put("pinned", t.pinned)
                    .put("archived", t.archived)
                    .put("createdAt", t.createdAt)
                    .put("updatedAt", t.updatedAt)
            )
        }
        val logs = JSONArray()
        data.logs.forEach { l ->
            logs.put(
                JSONObject()
                    .put("id", l.id)
                    .put("eventTypeId", l.eventTypeId)
                    .put("start", l.startTime)
                    .put("end", l.endTime ?: JSONObject.NULL)
                    .put("note", l.note ?: JSONObject.NULL)
                    .put("createdAt", l.createdAt)
                    .put("updatedAt", l.updatedAt)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("eventTypes", types)
            .put("logs", logs)
            .toString(2)
    }

    /** Parses a backup made by [toJson]; throws [BackupFormatException] on anything else. */
    fun fromJson(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw BackupFormatException("This file isn't a log.life backup.")
        }
        if (root.optString("format") != FORMAT) throw BackupFormatException("This file isn't a log.life backup.")
        if (root.optInt("version") > VERSION) throw BackupFormatException("This backup is from a newer version of log.life.")
        try {
            val types = root.getJSONArray("eventTypes").objects().map { o ->
                EventType(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    icon = o.optString("icon", "event"),
                    eventMode = EventMode.valueOf(o.getString("mode")),
                    category = o.optStringOrNull("category"),
                    showInWidget = o.optBoolean("showInWidget", false),
                    widgetOrder = o.optInt("widgetOrder", 0),
                    color = o.optStringOrNull("color"),
                    pinned = o.optBoolean("pinned", false),
                    archived = o.optBoolean("archived", false),
                    createdAt = o.optLong("createdAt", 0L),
                    updatedAt = o.optLong("updatedAt", 0L),
                )
            }
            val typeIds = types.map { it.id }.toSet()
            val logs = root.getJSONArray("logs").objects().map { o ->
                EventInstance(
                    id = o.getLong("id"),
                    eventTypeId = o.getLong("eventTypeId"),
                    startTime = o.getLong("start"),
                    endTime = if (o.isNull("end")) null else o.getLong("end"),
                    note = o.optStringOrNull("note"),
                    createdAt = o.optLong("createdAt", 0L),
                    updatedAt = o.optLong("updatedAt", 0L),
                )
            }.filter { it.eventTypeId in typeIds }
            return BackupData(types, logs)
        } catch (e: Exception) {
            throw BackupFormatException("This backup is damaged and can't be restored.")
        }
    }

    /** One row per log, newest last. Times are local ISO-8601. */
    fun toCsv(data: BackupData, zone: ZoneId = ZoneId.systemDefault()): String {
        val types = data.eventTypes.associateBy { it.id }
        val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        fun time(millis: Long) = iso.format(Instant.ofEpochMilli(millis).atZone(zone))
        val out = StringBuilder("event,category,type,start,end,duration_minutes,note\n")
        data.logs.sortedBy { it.startTime }.forEach { l ->
            val t = types[l.eventTypeId]
            val isSession = t?.eventMode == EventMode.DURATION
            val minutes = if (isSession && l.endTime != null) ((l.endTime - l.startTime) / 60_000).toString() else ""
            out.append(
                listOf(
                    t?.name.orEmpty(),
                    t?.category.orEmpty(),
                    if (isSession) "session" else "moment",
                    time(l.startTime),
                    l.endTime?.takeIf { isSession }?.let(::time).orEmpty(),
                    minutes,
                    l.note.orEmpty(),
                ).joinToString(",") { csvField(it) }
            ).append('\n')
        }
        return out.toString()
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)
}
