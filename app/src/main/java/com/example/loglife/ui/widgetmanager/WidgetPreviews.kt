package com.example.loglife.ui.widgetmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.data.settings.ThemeMode
import com.example.loglife.data.settings.WidgetSettings
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.widgets.WidgetData
import com.example.loglife.ui.widgets.WidgetKind
import com.example.loglife.ui.widgets.WidgetStyle
import com.example.loglife.util.formatClock
import com.example.loglife.util.formatElapsedLong
import kotlinx.coroutines.delay

/** Widget colours for the chosen widget theme, as the launcher would draw them. */
internal data class WidgetColors(val card: Color, val ink: Color, val onInk: Color, val muted: Color, val tile: Color, val dark: Boolean)

@Composable
internal fun widgetColors(settings: WidgetSettings): WidgetColors {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    fun pick(p: WidgetStyle.DayNight) = Color(if (dark) p.night else p.day)
    return WidgetColors(
        card = pick(WidgetStyle.CARD).copy(alpha = settings.opacity / 100f),
        ink = pick(WidgetStyle.INK),
        onInk = pick(WidgetStyle.ON_INK),
        muted = if (dark) Color(0xFF9A9DA6) else Color(0xFF6B6E76),
        tile = if (dark) Color(0xFF2A2D36) else Color(0xFFFFFFFF),
        dark = dark,
    )
}

@Composable
private fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000 - value % 1_000)
        }
    }
    return now
}

/** A slice of home-screen wallpaper the preview sits on. */
@Composable
internal fun Wallpaper(modifier: Modifier = Modifier, description: String, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF5B6A5E))
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0xFF66786A), radius = size.width * 0.55f, center = Offset(size.width * 1.02f, size.height * 1.1f))
        }
        Box(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun WidgetCard(colors: WidgetColors, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .padding(14.dp)
    ) { content() }
}

@Composable
internal fun DeckPreview(tiles: List<WidgetData.Tile>, settings: WidgetSettings) {
    val colors = widgetColors(settings)
    val now = rememberNow()
    val tile = tiles.firstOrNull()
    Wallpaper(description = "Preview of the quick log deck") {
        WidgetCard(colors) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (tile?.isRunning == true) "RUNNING" else "QUICK LOG",
                        style = sheetText(12.sp, FontWeight.Bold, colors.muted, letterSpacing = 1.2.sp),
                        modifier = Modifier.weight(1f)
                    )
                    if (tiles.isNotEmpty()) Text("1 / ${tiles.size}", style = sheetMono(13.sp, color = colors.muted))
                }
                Spacer(Modifier.height(10.dp))
                if (tile == null) {
                    EmptyTile(colors, "Pick favourites")
                } else {
                    val running = tile.isRunning
                    val bg = if (running) colors.ink else Color(
                        WidgetStyle.blend(tile.color, if (colors.dark) WidgetStyle.CARD.night else WidgetStyle.CARD.day, if (colors.dark) 0.24f else 0.18f)
                    )
                    val text = if (running) colors.onInk else colors.ink
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(bg)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EventMarker(Color(tile.color), tile.isSession || running, 11.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(tile.name, style = sheetText(19.sp, FontWeight.Bold, text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (running) {
                                Text(formatElapsedLong(now - tile.runningSince!!), style = sheetMono(30.sp, FontWeight.Medium, text), modifier = Modifier.weight(1f))
                                Text("Tap to stop", style = sheetText(13.sp, FontWeight.SemiBold, text.copy(alpha = 0.8f)))
                            } else {
                                Text(WidgetData.formatLast(tile.lastStart, now), style = sheetMono(13.sp, color = text.copy(alpha = 0.7f)), modifier = Modifier.weight(1f))
                                Text(if (tile.isSession) "Tap to start" else "Tap to log", style = sheetText(13.sp, FontWeight.SemiBold, text))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ButtonsPreview(buttons: List<WidgetData.Tile>, settings: WidgetSettings) {
    val colors = widgetColors(settings)
    val now = rememberNow()
    Wallpaper(description = "Preview of the quick buttons") {
        WidgetCard(colors) {
            if (buttons.isEmpty()) {
                EmptyTile(colors, "Pick favourites")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    buttons.forEach { b ->
                        val running = b.isRunning
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 84.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (running) colors.ink else colors.tile)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            EventMarker(Color(b.color), b.isSession, 16.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (running) formatElapsedLong(now - b.runningSince!!) else b.name,
                                style = if (running) sheetMono(12.sp, FontWeight.Medium, colors.onInk) else sheetText(13.sp, FontWeight.Bold, colors.ink),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TodayPreview(today: WidgetData.Today, settings: WidgetSettings) {
    val colors = widgetColors(settings)
    val now = rememberNow()
    Wallpaper(description = "Preview of the Today widget") {
        WidgetCard(colors) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Today", style = sheetText(20.sp, FontWeight.Bold, colors.ink), modifier = Modifier.weight(1f))
                    if (settings.todayShowTotal) {
                        Text(
                            "${today.count} ${if (today.count == 1) "log" else "logs"} · ${WidgetData.formatTotal(today.totalLoggedMillis)}",
                            style = sheetMono(13.sp, color = colors.muted)
                        )
                    }
                }
                today.running.take(3).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.ink)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(row.color)))
                        Spacer(Modifier.width(10.dp))
                        Text(row.name, style = sheetText(15.sp, FontWeight.SemiBold, colors.onInk), modifier = Modifier.weight(1f), maxLines = 1)
                        Text(formatElapsedLong(now - row.startTime), style = sheetMono(14.sp, color = colors.onInk))
                    }
                }
                today.logged.take(settings.todayRows).forEach { row ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                        Text(formatClock(row.startTime), style = sheetMono(13.sp, color = colors.muted), modifier = Modifier.width(56.dp))
                        EventMarker(Color(row.color), row.isSession, 9.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(row.name, style = sheetText(15.sp, FontWeight.SemiBold, colors.ink), modifier = Modifier.weight(1f), maxLines = 1)
                        if (row.isSession && row.endTime != null) {
                            Text(WidgetData.formatTotal(row.endTime - row.startTime), style = sheetMono(13.sp, color = colors.ink))
                        }
                    }
                }
                if (today.count == 0) Text("Nothing logged yet", style = sheetText(13.sp, color = colors.muted))
            }
        }
    }
}

/** One idle tile on wallpaper, for the Look section. */
@Composable
internal fun LookPreview(tile: WidgetData.Tile?, settings: WidgetSettings) {
    val colors = widgetColors(settings)
    val now = rememberNow()
    Wallpaper(description = "Preview of the widget background") {
        WidgetCard(colors) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.tile)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventMarker(Color(tile?.color ?: 0xFFC98A1E.toInt()), tile?.isSession ?: false, 10.dp)
                Spacer(Modifier.width(10.dp))
                Text(tile?.name ?: "Coffee", style = sheetText(16.sp, FontWeight.Bold, colors.ink), modifier = Modifier.weight(1f))
                Text(WidgetData.formatLast(tile?.lastStart ?: (now - 3_600_000L), now), style = sheetMono(12.sp, color = colors.muted))
            }
        }
    }
}

@Composable
private fun EmptyTile(colors: WidgetColors, label: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.tile),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(15.sp, FontWeight.SemiBold, colors.ink))
    }
}

/** Small pictogram of each widget for the "On your home screen" list. */
@Composable
internal fun WidgetThumbnail(kind: WidgetKind, size: Dp = 56.dp) {
    val frame = Color(0xFF5B6A5E)
    val card = Color(0xFFF3F3EF)
    val ink = Color(0xFF16181D)
    Box(
        Modifier
            .width(size)
            .height(size * 0.72f)
            .clip(RoundedCornerShape(10.dp))
            .background(frame)
            .padding(4.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(card)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            when (kind) {
                WidgetKind.DECK -> {
                    drawRoundRect(ink, topLeft = Offset(w * 0.1f, h * 0.14f), size = Size(w * 0.8f, h * 0.5f), cornerRadius = CornerRadius(6f))
                    listOf(0.42f, 0.5f, 0.58f).forEach { x -> drawCircle(ink.copy(alpha = 0.5f), radius = h * 0.045f, center = Offset(w * x, h * 0.8f)) }
                }
                WidgetKind.BUTTONS -> {
                    listOf(Color(0xFF4C6FE0), Color(0xFFC98A1E), Color(0xFF2E9E6B), Color(0xFFC2508A)).forEachIndexed { i, c ->
                        val center = Offset(w * (0.17f + i * 0.22f), h * 0.5f)
                        if (i % 2 == 0) drawCircle(c, radius = h * 0.14f, center = center)
                        else drawCircle(c, radius = h * 0.12f, center = center, style = Stroke(width = h * 0.06f))
                    }
                }
                WidgetKind.TILE -> {
                    val c = Color(0xFFC98A1E)
                    val side = h * 0.78f
                    val left = (w - side) / 2f
                    drawRoundRect(Color(0xFFEDE3CF), topLeft = Offset(left, h * 0.11f), size = Size(side, side), cornerRadius = CornerRadius(8f))
                    drawCircle(c, radius = h * 0.08f, center = Offset(left + side * 0.28f, h * 0.11f + side * 0.28f), style = Stroke(width = h * 0.045f))
                    drawRoundRect(ink, topLeft = Offset(left + side * 0.18f, h * 0.11f + side * 0.62f), size = Size(side * 0.6f, side * 0.12f), cornerRadius = CornerRadius(3f))
                }
                WidgetKind.TODAY -> {
                    drawRoundRect(ink, topLeft = Offset(w * 0.1f, h * 0.16f), size = Size(w * 0.5f, h * 0.12f), cornerRadius = CornerRadius(3f))
                    drawRoundRect(ink, topLeft = Offset(w * 0.1f, h * 0.38f), size = Size(w * 0.8f, h * 0.14f), cornerRadius = CornerRadius(3f))
                    listOf(0.62f, 0.78f).forEach { y ->
                        drawRoundRect(ink.copy(alpha = 0.18f), topLeft = Offset(w * 0.1f, h * y), size = Size(w * 0.8f, h * 0.08f), cornerRadius = CornerRadius(3f))
                    }
                }
            }
        }
    }
}
