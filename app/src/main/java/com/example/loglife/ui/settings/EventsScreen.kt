package com.example.loglife.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.components.SectionLabel
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.theme.EventSwatches
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.SheetBorder
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The eight colours offered when recolouring (a subset of [EventSwatches]). */
private val RecolourSwatches = listOf("Blue", "Green", "Amber", "Violet", "Teal", "Rose", "Red", "Gray")
    .mapNotNull { label -> EventSwatches.firstOrNull { it.label == label } }

private val archivedDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private const val NO_CATEGORY = "No category"

@Composable
fun EventsScreen(onBack: () -> Unit, viewModel: EventsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    EventsContent(
        state = state,
        onBack = onBack,
        onEdit = { editingId = it },
        onRenameCategory = { renaming = it },
        onRestore = viewModel::restore
    )

    val editing = editingId?.let { id -> (state.groups.flatMap { it.events } + state.archived).firstOrNull { it.type.id == id } }
    if (editing != null) {
        EditEventSheet(
            row = editing,
            categories = state.categoryNames,
            onDismiss = { editingId = null },
            onArchive = {
                viewModel.archive(editing.type.id)
                editingId = null
            },
            onSave = { name, category, color ->
                scope.launch {
                    if (viewModel.save(editing.type.id, name, category, color)) editingId = null
                }
            },
            checkName = { name ->
                val clean = name.trim().lowercase()
                (state.groups.flatMap { it.events } + state.archived).none { it.type.id != editing.type.id && it.type.name.lowercase() == clean }
            }
        )
    }

    renaming?.let { old ->
        RenameCategoryDialog(
            current = old,
            onDismiss = { renaming = null },
            onRename = { new ->
                viewModel.renameCategory(old, new)
                renaming = null
            }
        )
    }
}

@Composable
fun EventsContent(
    state: EventsState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRenameCategory: (String) -> Unit,
    onRestore: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    SettingsPage(
        title = "Events & categories",
        backLabel = "Settings",
        onBack = onBack,
        subtitle = "Tap an event to rename, recolour or archive it."
    ) {
        Spacer(Modifier.height(4.dp))
        if (state.groups.isEmpty() && state.archived.isEmpty()) {
            Text(
                "No events yet. Create one from the log sheet on Today.",
                style = sheetText(15.sp, color = HomeInkSecondary),
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.groups.forEach { group ->
                val key = group.name ?: NO_CATEGORY
                CategoryCard(
                    group = group,
                    expanded = key in expanded,
                    onToggle = { expanded = if (key in expanded) expanded - key else expanded + key },
                    onRename = group.name?.let { name -> { onRenameCategory(name) } },
                    onEdit = onEdit
                )
            }
        }
        if (state.archived.isNotEmpty()) {
            SectionLabel("Archived · ${state.archived.size}")
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(HomeSurface)
            ) {
                state.archived.forEachIndexed { i, row ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(SheetDivider))
                    ArchivedRow(row, onRestore = { onRestore(row.type.id) }, onEdit = { onEdit(row.type.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    group: CategoryGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRename: (() -> Unit)?,
    onEdit: (Long) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeSurface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClick = onToggle)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HomeInk,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 90f else 0f)
            )
            Spacer(Modifier.width(12.dp))
            Text(group.name ?: NO_CATEGORY, style = sheetText(17.sp, FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
            Text(group.events.size.toString(), style = sheetText(15.sp, color = HomeInkSecondary))
            Spacer(Modifier.weight(1f))
            if (onRename != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Rename ${group.name}", onClick = onRename)
                        .semantics { contentDescription = "Rename category ${group.name}" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = HomeInk, modifier = Modifier.size(20.dp))
                }
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                group.events.forEach { row ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SheetDivider))
                    EventRowItem(row, onClick = { onEdit(row.type.id) })
                }
            }
        }
    }
}

@Composable
private fun EventRowItem(row: EventRow, onClick: () -> Unit) {
    val type = row.type
    val isSession = type.eventMode == EventMode.DURATION
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = "Edit ${type.name}", onClick = onClick)
            .padding(start = 46.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventMarker(eventColor(type.color, type.category), isSession)
        Spacer(Modifier.width(14.dp))
        Text(type.name, style = sheetText(16.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1)
        Text(if (isSession) "Session" else "Moment", style = sheetText(14.sp, color = HomeInkSecondary))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ArchivedRow(row: EventRow, onRestore: () -> Unit, onEdit: () -> Unit) {
    val type = row.type
    val date = archivedDate.format(Instant.ofEpochMilli(type.updatedAt).atZone(ZoneId.systemDefault()))
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClickLabel = "Edit ${type.name}", onClick = onEdit)
            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventMarker(HomeInkMuted, isSession = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(type.name, style = sheetText(16.sp, FontWeight.SemiBold, HomeInkSecondary), maxLines = 1)
            Text(
                listOfNotNull(type.category, "archived $date").joinToString(" · "),
                style = sheetText(13.sp, color = HomeInkSecondary)
            )
        }
        Box(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SheetMuted)
                .clickable(onClickLabel = "Restore ${type.name}", onClick = onRestore)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Restore", style = sheetText(15.sp, FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditEventSheet(
    row: EventRow,
    categories: List<String>,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    onSave: (name: String, category: String?, colorHex: String?) -> Unit,
    checkName: (String) -> Boolean,
) {
    val type = row.type
    val isSession = type.eventMode == EventMode.DURATION
    var name by remember(type.id) { mutableStateOf(type.name) }
    var category by remember(type.id) { mutableStateOf(type.category) }
    var colorHex by remember(type.id) { mutableStateOf(type.color) }
    val nameOk = name.isNotBlank() && checkName(name)
    val options = (categories + listOfNotNull(type.category)).distinct()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HomeBackground
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit event", style = sheetText(26.sp, FontWeight.Bold, letterSpacing = (-0.3).sp), modifier = Modifier.weight(1f))
                EventMarker(eventColor(colorHex, category), isSession, 11.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${if (isSession) "Session" else "Moment"} · ${row.logCount} ${if (row.logCount == 1) "log" else "logs"}",
                    style = sheetText(15.sp, color = HomeInkSecondary)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Name", style = sheetText(14.sp, FontWeight.SemiBold, HomeInkSecondary))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HomeSurface)
                        .border(2.dp, HomeInk, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = sheetText(18.sp, FontWeight.SemiBold),
                        cursorBrush = SolidColor(HomeInk),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Event name" }
                    )
                }
                if (name.isNotBlank() && !checkName(name)) {
                    Text("Another event is already called “${name.trim()}”.", style = sheetText(13.sp, color = HomeAccent))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = sheetText(14.sp, FontWeight.SemiBold, HomeInkSecondary))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        CategoryChip(option, selected = category == option) { category = option }
                    }
                    CategoryChip("None", selected = category == null) { category = null }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Colour", style = sheetText(14.sp, FontWeight.SemiBold, HomeInkSecondary))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecolourSwatches.forEach { swatch ->
                        val on = colorHex.equals(swatch.hex, ignoreCase = true)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .then(if (on) Modifier.border(3.dp, HomeInk, RoundedCornerShape(12.dp)) else Modifier)
                                .padding(if (on) 3.dp else 0.dp)
                                .clip(RoundedCornerShape(if (on) 9.dp else 12.dp))
                                .background(swatch.color)
                                .selectable(selected = on, role = Role.RadioButton) { colorHex = swatch.hex }
                                .semantics { contentDescription = swatch.label },
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text(
                    "Past logs keep their history; only the colour and name change.",
                    style = sheetText(13.sp, color = HomeInkSecondary)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(HomeSurface)
                        .clickable(enabled = !type.archived, onClick = onArchive),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Archive, contentDescription = null, tint = HomeInk, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (type.archived) "Archived" else "Archive", style = sheetText(17.sp, FontWeight.SemiBold))
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (nameOk) HomeInk else SheetMuted)
                        .clickable(enabled = nameOk) { onSave(name, category, colorHex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save", style = sheetText(17.sp, FontWeight.Bold, if (nameOk) HomeBackground else HomeInkSecondary))
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) HomeInk else HomeSurface)
            .border(1.dp, if (selected) HomeInk else SheetBorder, RoundedCornerShape(22.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(16.sp, FontWeight.SemiBold, if (selected) HomeBackground else HomeInk))
    }
}

@Composable
private fun RenameCategoryDialog(current: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Every event in “$current” moves to the new name.", style = sheetText(14.sp, color = HomeInkSecondary))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HomeSurface)
                        .border(2.dp, HomeInk, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = sheetText(17.sp, FontWeight.SemiBold),
                        cursorBrush = SolidColor(HomeInk),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Category name" }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRename(name) }, enabled = name.isNotBlank()) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun EventsContentPreview() {
    fun t(id: Long, name: String, cat: String?, mode: EventMode, archived: Boolean = false) =
        EventType(id = id, name = name, icon = "event", eventMode = mode, category = cat, archived = archived)
    val types = listOf(
        t(1, "Deep work", "Work", EventMode.DURATION), t(2, "Meetings", "Work", EventMode.DURATION),
        t(3, "Coffee", "Food", EventMode.INSTANT), t(4, "Lunch", "Food", EventMode.DURATION),
        t(5, "Green tea", "Food", EventMode.INSTANT, archived = true),
    )
    LoglifeTheme {
        EventsContent(buildEventsState(types, emptyList()), onBack = {}, onEdit = {}, onRenameCategory = {}, onRestore = {})
    }
}
