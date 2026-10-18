package com.example.loglife.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.theme.EventSwatches
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.SheetMuted
import kotlinx.coroutines.launch
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.components.PillSwitch

@Composable
internal fun NewEventDialog(
    initialName: String,
    initialFolder: String?,
    folders: List<String>,
    folderColor: (String) -> Color?,
    onDismiss: () -> Unit,
    /** Edit mode: prefills from this event, hides the type choice (existing logs depend on it) and says Save. */
    existing: EventType? = null,
    onCreate: suspend (name: String, isSession: Boolean, colorHex: String, folder: String?, pinned: Boolean) -> Boolean
) {
    var name by remember { mutableStateOf(initialName) }
    var isSession by remember { mutableStateOf(existing?.eventMode?.let { it == EventMode.DURATION } ?: true) }
    var swatch by remember {
        mutableStateOf(
            existing?.color?.let { hex -> EventSwatches.firstOrNull { it.hex.equals(hex, ignoreCase = true) } }
                ?: EventSwatches.first()
        )
    }
    var folder by remember { mutableStateOf(initialFolder) }
    var newFolderMode by remember { mutableStateOf(false) }
    var pinned by remember { mutableStateOf(existing?.pinned ?: false) }
    var duplicate by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val nameFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { if (initialName.isEmpty()) nameFocus.requestFocus() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .shadow(30.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(HomeBackground)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "New event" else "Edit event", style = sheetText(22.sp, FontWeight.Bold, letterSpacing = (-0.2).sp), modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SheetMuted)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Name")
                OutlinedInput(
                    value = name,
                    onValueChange = { name = it; duplicate = false },
                    placeholder = "Yoga, Coffee, Commute…",
                    highlighted = true,
                    modifier = Modifier.focusRequester(nameFocus),
                    description = "Event name"
                )
                if (duplicate) {
                    Text(
                        "An event called “${name.trim()}” already exists. Search for it instead, or pick another name.",
                        style = sheetText(12.sp, color = HomeAccent, lineHeight = 16.sp)
                    )
                }
            }

            if (existing == null) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Type")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeCard("Moment", "One point in time, like waking up", isSession = false, selected = !isSession, color = swatch.color, modifier = Modifier.weight(1f)) { isSession = false }
                    TypeCard("Session", "Has a start and an end, like work", isSession = true, selected = isSession, color = swatch.color, modifier = Modifier.weight(1f)) { isSession = true }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Color")
                EventSwatches.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { s ->
                            val on = s == swatch
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (on) s.color else Color.Transparent)
                                    .selectable(selected = on, role = Role.RadioButton, onClick = { swatch = s })
                                    .semantics { contentDescription = s.label },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(if (on) 36.dp else 34.dp)
                                        .then(if (on) Modifier.border(3.dp, HomeBackground, CircleShape) else Modifier)
                                        .padding(if (on) 3.dp else 0.dp)
                                        .clip(CircleShape)
                                        .background(s.color)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(HomeSurface)
            ) {
                if (newFolderMode) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("New folder", style = sheetText(15.sp, FontWeight.SemiBold))
                        OutlinedInput(
                            value = folder.orEmpty(),
                            onValueChange = { folder = it },
                            placeholder = "Folder name",
                            highlighted = false,
                            description = "New folder name"
                        )
                    }
                } else {
                    FolderRow(
                        folder = folder,
                        folders = folders,
                        color = folder?.let(folderColor) ?: swatch.color,
                        onPick = { folder = it },
                        onNewFolder = { folder = ""; newFolderMode = true }
                    )
                }
                Box(
                    Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SheetDivider)
                )
                Row(
                    Modifier
                        .heightIn(min = 54.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Pin to top", style = sheetText(15.sp, FontWeight.SemiBold))
                        Text("Shows in Pinned on the Log sheet", style = sheetText(12.sp, color = HomeInkSecondary))
                    }
                    PillSwitch(
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                        modifier = Modifier.semantics { contentDescription = "Pin to top" }
                    )
                }
            }

            AccentButton(
                label = if (existing == null) "Create" else "Save",
                enabled = name.isNotBlank() && !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        val ok = onCreate(name, isSession, swatch.hex, folder?.takeIf { it.isNotBlank() }, pinned)
                        saving = false
                        if (!ok) duplicate = true
                    }
                }
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text.uppercase(), style = sheetText(13.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp))
}

@Composable
private fun OutlinedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    highlighted: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeSurface)
            .border(2.dp, if (highlighted) HomeInk else SheetDivider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder, style = sheetText(17.sp, color = Color(0xFF7A7F88)))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = sheetText(17.sp, FontWeight.SemiBold),
            cursorBrush = SolidColor(HomeInk),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
        )
    }
}

@Composable
private fun TypeCard(
    title: String,
    body: String,
    isSession: Boolean,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HomeSurface)
            .border(2.dp, if (selected) HomeInk else Color.Transparent, RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EventMarker(if (selected) color else HomeInkSecondary, isSession)
            Spacer(Modifier.width(8.dp))
            Text(title, style = sheetText(15.sp, FontWeight.Bold))
        }
        Text(body, style = sheetText(12.sp, color = HomeInkSecondary, lineHeight = 16.sp))
    }
}

@Composable
private fun FolderRow(
    folder: String?,
    folders: List<String>,
    color: Color,
    onPick: (String?) -> Unit,
    onNewFolder: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable { open = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Folder", style = sheetText(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (folder != null) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                folder ?: "None",
                style = sheetText(15.sp, color = HomeInkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { open = false; onPick(null) })
            folders.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { open = false; onPick(name) })
            }
            DropdownMenuItem(text = { Text("New folder…") }, onClick = { open = false; onNewFolder() })
        }
    }
}
