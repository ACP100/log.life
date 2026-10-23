package com.example.loglife.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loglife.data.model.Note
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetEmptyMini
import com.example.loglife.ui.theme.SheetLink
import com.example.loglife.ui.theme.categoryColor
import com.example.loglife.util.formatClock
import com.example.loglife.util.formatDurationShort

/** Scrim from the design: rgba(22,24,29,0.45). */
private val PopupScrim = Color(0x7316181D)

/**
 * Centred pop-up for one log: title, one time line, notes, and an add-note row.
 * Closed with × or a tap on the scrim. No delete here; that is a row swipe.
 */
@Composable
internal fun LogDetailPopup(
    item: LoggedItem,
    notes: List<Note>,
    context: LogContext?,
    onAddNote: (String) -> Unit,
    onEditTime: () -> Unit,
    onOpenStats: () -> Unit,
    onLogAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PopupScrim)
                .clickable(onClickLabel = "Close", role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(HomeBackground)
                    .pointerInput(Unit) { detectTapGestures { } } // taps on the card don't reach the scrim
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)
            ) {
                TitleRow(item, onDismiss)
                Spacer(Modifier.height(10.dp))
                TimeLine(item, onEditTime)
                if (notes.isEmpty() && context != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(contextLine(context), style = text(13.sp, color = HomeInkSecondary))
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                notes.forEach { note ->
                    NoteRow(note)
                    Divider()
                }
                AddNoteRow(
                    placeholder = if (notes.isEmpty()) "No notes yet. Add one…" else "Add a note…",
                    onSubmit = onAddNote
                )
                Divider()
                if (notes.isEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HomeSurface)
                            .clickable(role = Role.Button, onClickLabel = "Log ${item.name} again", onClick = onLogAgain),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Log ${item.name} again", style = text(15.sp, FontWeight.SemiBold, HomeInk))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "${item.name} stats",
                    style = text(14.sp, FontWeight.SemiBold, SheetLink),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onOpenStats)
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TitleRow(item: LoggedItem, onDismiss: () -> Unit) {
    val color = categoryColor(item.category)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (item.isDuration) Dot(color, 10.dp) else Ring(color, 10.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            item.name,
            style = text(22.sp, FontWeight.Bold, HomeInk),
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        item.category?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, style = text(13.sp, color = HomeInkSecondary), maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClickLabel = "Close", onClick = onDismiss)
                .semantics { contentDescription = "Close" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TimeLine(item: LoggedItem, onEditTime: () -> Unit) {
    val line = buildAnnotatedString {
        if (item.isDuration && item.endTime != null) {
            append("${formatClock(item.startTime)} – ${formatClock(item.endTime)} · ")
            withStyle(SpanStyle(color = HomeInkSecondary)) {
                append(formatDurationShort(item.endTime - item.startTime))
            }
        } else {
            append("${formatClock(item.startTime)} · moment")
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = "Edit time", onClick = onEditTime)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(line, style = mono(15.sp, color = HomeInk), modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Edit, contentDescription = null, tint = HomeInkMuted, modifier = Modifier.size(16.dp))
    }
}

private fun contextLine(context: LogContext): String = buildList {
    if (context.timesThisWeek > 0) add("${ordinal(context.timesThisWeek)} this week")
    context.usualStart?.let { add("usually around ${it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}") }
}.joinToString(" · ")

@Composable
private fun NoteRow(note: Note) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(formatClock(note.createdAt), style = mono(13.sp, color = HomeInkSecondary), modifier = Modifier.width(42.dp))
        Spacer(Modifier.width(10.dp))
        Text(note.text, style = text(15.sp, color = HomeInk), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AddNoteRow(placeholder: String, onSubmit: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    val submit = {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            onSubmit(text)
            draft = ""
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("now", style = mono(13.sp, color = HomeInkMuted), modifier = Modifier.width(42.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (draft.isEmpty()) {
                Text(placeholder, style = text(15.sp, color = HomeInkMuted))
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = text(15.sp, color = HomeInk),
                cursorBrush = SolidColor(HomeInk),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = placeholder }
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SheetEmptyMini))
}
