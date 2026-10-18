package com.example.loglife.ui.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.util.formatClock
import com.example.loglife.util.formatElapsedCompact
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkFill
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeStopTile
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetBorder
import com.example.loglife.ui.theme.SheetChipDark
import com.example.loglife.ui.theme.SheetEmptyMini
import com.example.loglife.ui.theme.SheetHandle
import com.example.loglife.ui.theme.SheetInkOnDark
import com.example.loglife.ui.theme.SheetLink
import com.example.loglife.ui.theme.eventColor
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.components.EventMarker

private const val TOAST_MILLIS = 4_000L

private data class NewEventRequest(val name: String = "", val folder: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(onDismiss: () -> Unit, viewModel: LogSheetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000 - value % 1_000)
        }
    }

    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var editingPins by rememberSaveable { mutableStateOf(false) }
    var newEvent by remember { mutableStateOf<NewEventRequest?>(null) }
    var logAt by remember { mutableStateOf<EventTile?>(null) }

    LaunchedEffect(Unit) { viewModel.onOpened() }
    LaunchedEffect(toast?.id) {
        val current = toast ?: return@LaunchedEffect
        delay(TOAST_MILLIS)
        viewModel.dismissToast(current.id)
    }

    val onTile: (EventTile) -> Unit = { tile ->
        if (editingPins) viewModel.setPinned(tile.typeId, tile.typeId !in state.pinned.map { it.typeId })
        else viewModel.onTileTap(tile)
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.setQuery("")
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = HomeBackground,
        contentColor = HomeInk,
        scrimColor = HomeInk.copy(alpha = 0.45f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SheetHandle)
            )
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LogSheetContent(
                state = state,
                query = query,
                now = now,
                editingPins = editingPins,
                onQueryChange = viewModel::setQuery,
                onNewEvent = { newEvent = NewEventRequest() },
                onCreateNamed = { newEvent = NewEventRequest(name = it) },
                onStop = viewModel::stop,
                onTile = onTile,
                onTileLongPress = { logAt = it },
                onFolder = { openFolder = it },
                onToggleEditPins = { editingPins = !editingPins },
                onRestore = viewModel::restore
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Brush.verticalGradient(listOf(HomeBackground.copy(alpha = 0f), HomeBackground)))
            )
            val current = toast
            if (current != null && openFolder == null) {
                SheetToastBar(
                    toast = current,
                    onUndo = viewModel::undo,
                    onPause = viewModel::pauseOther,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
                )
            }
        }
    }

    val folder = openFolder?.let { name -> state.folders.firstOrNull { it.name == name } }
    if (openFolder != null && folder == null) openFolder = null
    if (folder != null) {
        FolderDialog(
            folder = folder,
            now = now,
            toast = toast,
            editingPins = editingPins,
            pinnedIds = state.pinned.map { it.typeId }.toSet(),
            onDismiss = { openFolder = null },
            onTile = onTile,
            onTileLongPress = { logAt = it },
            onAddHere = { newEvent = NewEventRequest(folder = folder.name) },
            onRename = { viewModel.renameFolder(folder.name, it); openFolder = it.trim() },
            onUngroup = { viewModel.ungroupFolder(folder.name); openFolder = null },
            onUndo = viewModel::undo,
            onPause = viewModel::pauseOther
        )
    }

    newEvent?.let { request ->
        NewEventDialog(
            initialName = request.name,
            initialFolder = request.folder,
            folders = state.folders.map { it.name },
            folderColor = { name -> state.folders.firstOrNull { it.name == name }?.color() },
            onDismiss = { newEvent = null },
            onCreate = { name, isSession, colorHex, folderName, pinned ->
                val id = viewModel.createEvent(name, isSession, colorHex, folderName, pinned)
                if (id != null) {
                    newEvent = null
                    viewModel.setQuery("")
                }
                id != null
            }
        )
    }

    logAt?.let { tile ->
        LogAtTimeSheet(
            initial = tile,
            allEvents = state.allEvents,
            eventsOn = viewModel::eventsOn,
            onDismiss = { logAt = null },
            onArchive = { chosen ->
                viewModel.archive(chosen.typeId)
                logAt = null
            },
            onSave = { chosen, start, end, note ->
                viewModel.saveLog(chosen, start, end, note)
                logAt = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogSheetContent(
    state: LogSheetState,
    query: String,
    now: Long,
    editingPins: Boolean,
    onQueryChange: (String) -> Unit,
    onNewEvent: () -> Unit,
    onCreateNamed: (String) -> Unit,
    onStop: (Long) -> Unit,
    onTile: (EventTile) -> Unit,
    onTileLongPress: (EventTile) -> Unit,
    onFolder: (String) -> Unit,
    onToggleEditPins: () -> Unit,
    onRestore: (Long) -> Unit
) {
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchMode = searchFocused || query.isNotEmpty()
    val pinnedIds = state.pinned.map { it.typeId }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!searchMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Log something",
                    style = sheetText(22.sp, FontWeight.Bold, letterSpacing = (-0.2).sp),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HomeSurface)
                        .border(1.dp, SheetBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onNewEvent)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New event", style = sheetText(14.sp, FontWeight.SemiBold))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchField(
                query = query,
                active = searchMode,
                onQueryChange = onQueryChange,
                onFocusChange = { searchFocused = it },
                modifier = Modifier.weight(1f)
            )
            if (searchMode) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "Cancel",
                    style = sheetText(15.sp, FontWeight.SemiBold, SheetLink),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onQueryChange("")
                            focusManager.clearFocus()
                        }
                        .padding(horizontal = 2.dp, vertical = 12.dp)
                )
            }
        }

        val search = state.search
        if (search != null) {
            SearchResultsSection(
                results = search,
                now = now,
                pinnedIds = pinnedIds,
                editingPins = editingPins,
                onCreate = { onCreateNamed(search.query) },
                onTile = onTile,
                onTileLongPress = onTileLongPress,
                onFolder = onFolder,
                onRestore = onRestore
            )
        } else {

            if (state.running.isNotEmpty()) {
                state.running.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { RunningChipView(it, now, onStop = { onStop(it.instanceId) }, Modifier.weight(1f)) }
                        if (pair.size == 1 && state.running.size > 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Pinned") {
                    Text(
                        if (editingPins) "Done" else "Edit",
                        style = sheetText(13.sp, FontWeight.SemiBold, SheetLink),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleEditPins)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                when {
                    editingPins -> Text(
                        "Tap any event to pin or unpin it. Pinned events stay at the top.",
                        style = sheetText(12.sp, color = HomeInkSecondary, lineHeight = 17.sp)
                    )
                    state.pinned.isEmpty() -> Text(
                        "Nothing pinned yet. Tap Edit, then tap the events you log most.",
                        style = sheetText(12.sp, color = HomeInkSecondary, lineHeight = 17.sp)
                    )
                }
                if (state.pinned.isNotEmpty()) {
                    TileGrid(state.pinned, columns = 4) { tile, modifier ->
                        EventTileView(tile, now, modifier, editingPins, pinned = true, onClick = { onTile(tile) }, onLongClick = { onTileLongPress(tile) })
                    }
                }
            }

            if (state.frequent.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Frequent") {
                        Text("last 30 days", style = sheetText(12.sp, color = HomeInkSecondary))
                    }
                    TileGrid(state.frequent, columns = 4) { tile, modifier ->
                        EventTileView(tile, now, modifier, editingPins, pinned = false, onClick = { onTile(tile) }, onLongClick = { onTileLongPress(tile) })
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("All events")
                val cells: List<Any> = state.folders + state.loose
                if (cells.isEmpty()) {
                    Text(
                        "No events yet. Tap New event to make your first one.",
                        style = sheetText(12.sp, color = HomeInkSecondary)
                    )
                }
                TileGrid(cells, columns = 4) { cell, modifier ->
                    when (cell) {
                        is FolderTile -> FolderTileView(cell, modifier, onClick = { onFolder(cell.name) })
                        is EventTile -> EventTileView(
                            cell, now, modifier, editingPins, pinned = cell.typeId in pinnedIds,
                            onClick = { onTile(cell) }, onLongClick = { onTileLongPress(cell) }
                        )
                    }
                }
                Text(
                    "Hold any event to log it at a different time.",
                    style = sheetText(12.sp, color = HomeInkSecondary),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    active: Boolean,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeSurface)
            .then(if (active) Modifier.border(2.dp, HomeInk, RoundedCornerShape(14.dp)) else Modifier)
            .clickable { focusRequester.requestFocus() }
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = if (active) HomeInk else HomeInkSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Search events and folders", style = sheetText(15.sp, color = Color(0xFF7A7F88)))
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = sheetText(15.sp, FontWeight.Medium),
                cursorBrush = SolidColor(HomeInk),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.isFocused) }
                    .semantics { contentDescription = "Search events and folders" }
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(HomeStopTile)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = HomeInkSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    results: SearchResults,
    now: Long,
    pinnedIds: Set<Long>,
    editingPins: Boolean,
    onCreate: () -> Unit,
    onTile: (EventTile) -> Unit,
    onTileLongPress: (EventTile) -> Unit,
    onFolder: (String) -> Unit,
    onRestore: (Long) -> Unit
) {
    if (results.folders.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Folders")
            TileGrid(results.folders, columns = 4) { folder, modifier ->
                FolderTileView(folder, modifier, onClick = { onFolder(folder.name) })
            }
        }
    }
    if (results.events.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Events")
            TileGrid(results.events, columns = 4) { tile, modifier ->
                EventTileView(
                    tile, now, modifier, editingPins, pinned = tile.typeId in pinnedIds,
                    onClick = { onTile(tile) }, onLongClick = { onTileLongPress(tile) }
                )
            }
        }
    }
    if (!results.exactMatch) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(if (results.isEmpty) "No event called “${results.query}”" else "Something new")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HomeSurface)
                    .clickable(onClick = onCreate)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HomeAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Create “${results.query}”",
                        style = sheetText(16.sp, FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Pick Moment or Session, color and folder", style = sheetText(13.sp, color = HomeInkSecondary))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = HomeInkSecondary)
            }
        }
    }
    if (results.archived.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Archived")
            results.archived.forEach { row -> ArchivedRowView(row, onRestore = { onRestore(row.typeId) }) }
        }
    }
    Text(
        "Search looks at event names, folder names and archived events, so old history can come back instead of being duplicated.",
        style = sheetText(12.sp, color = HomeInkSecondary, lineHeight = 17.sp)
    )
}

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())
private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

@Composable
private fun ArchivedRowView(row: ArchivedRow, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(HomeSurface)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(eventColor(row.colorHex, row.folder).copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = sheetText(15.sp, FontWeight.SemiBold, HomeInkSecondary), maxLines = 1)
            val last = row.lastStart?.let {
                val month = YearMonth.from(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                val label = if (month.year == YearMonth.now().year) monthFormatter.format(month) else monthYearFormatter.format(month)
                "last in $label"
            }
            val logs = if (row.total == 1) "1 log" else "${row.total} logs"
            Text(
                listOfNotNull(row.folder, if (row.isSession) "Session" else "Moment", listOfNotNull(logs, last).joinToString(", "))
                    .joinToString(" · "),
                style = sheetText(12.sp, color = HomeInkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HomeBackground)
                .clickable(onClick = onRestore)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Restore", style = sheetText(13.sp, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun RunningChipView(chip: RunningChip, now: Long, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HomeInkFill)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(eventColor(chip.colorHex, chip.folder))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                chip.name,
                style = sheetText(13.sp, FontWeight.SemiBold, Color(0xFFF3F3EF)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(formatElapsedCompact(now - chip.startTime), style = sheetMono(12.sp, color = SheetInkOnDark))
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SheetChipDark)
                .clickable(onClickLabel = "Stop ${chip.name}", onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFF3F3EF))
            )
        }
    }
}

@Composable
private fun <T> TileGrid(
    items: List<T>,
    columns: Int,
    gap: androidx.compose.ui.unit.Dp = 8.dp,
    cell: @Composable (T, Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { cell(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventTileView(
    tile: EventTile,
    now: Long,
    modifier: Modifier,
    editingPins: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 84.dp,
    sub: String? = tile.runningSince?.let { formatElapsedCompact(now - it) },
    highlighted: Boolean = false,
    tint: Float = 0.12f
) {
    val color = eventColor(tile.colorHex, tile.folder)
    val running = tile.isRunning
    val shape = RoundedCornerShape(16.dp)
    val action = when {
        editingPins -> if (pinned) "Unpin ${tile.name}" else "Pin ${tile.name}"
        running -> "Stop ${tile.name}"
        tile.isSession -> "Start ${tile.name}"
        else -> "Log ${tile.name}"
    }
    Column(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (running) HomeInkFill else color.copy(alpha = if (highlighted) 0.2f else tint))
            .then(
                when {
                    highlighted -> Modifier.border(2.dp, color, shape)
                    editingPins && pinned -> Modifier.border(2.dp, HomeInk, shape)
                    else -> Modifier
                }
            )
            .combinedClickable(
                onClickLabel = action,
                onLongClickLabel = "Log ${tile.name} at a different time",
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EventMarker(color, tile.isSession)
            Spacer(Modifier.weight(1f))
            if (editingPins) {
                Text(
                    if (pinned) "−" else "+",
                    style = sheetText(14.sp, FontWeight.Bold, if (running) Color.White else HomeInk)
                )
            }
        }
        Column {
            Text(
                tile.name,
                style = sheetText(13.sp, FontWeight.SemiBold, if (running) Color(0xFFF3F3EF) else HomeInk, lineHeight = 15.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (sub != null) {
                Text(
                    sub,
                    style = sheetMono(11.sp, color = if (running) SheetInkOnDark else HomeInkSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

internal fun FolderTile.color(): Color = events.firstOrNull()?.let { eventColor(it.colorHex, name) } ?: eventColor(null, name)

@Composable
private fun FolderTileView(folder: FolderTile, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HomeSurface)
            .clickable(onClickLabel = "Open ${folder.name}", onClick = onClick)
            .semantics { contentDescription = "${folder.name} folder, ${folder.events.size} events" }
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val minis = folder.events.take(4).map { eventColor(it.colorHex, folder.name) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (0 until 4).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pair.forEach { i ->
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(minis.getOrElse(i) { SheetEmptyMini })
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                folder.name,
                style = sheetText(13.sp, FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Text("${folder.events.size}", style = sheetMono(11.sp, color = HomeInkSecondary))
        }
    }
}

@Composable
private fun FolderDialog(
    folder: FolderTile,
    now: Long,
    toast: SheetToast?,
    editingPins: Boolean,
    pinnedIds: Set<Long>,
    onDismiss: () -> Unit,
    onTile: (EventTile) -> Unit,
    onTileLongPress: (EventTile) -> Unit,
    onAddHere: () -> Unit,
    onRename: (String) -> Unit,
    onUngroup: () -> Unit,
    onUndo: (SheetToast.Logged) -> Unit,
    onPause: (SheetToast.AlsoRunning) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val color = folder.color()
    val justLogged = (toast as? SheetToast.Logged)?.takeIf { !it.started }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 20.dp, end = 20.dp, top = 142.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(HomeSurface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        folder.name,
                        style = sheetText(24.sp, FontWeight.Bold, letterSpacing = (-0.2).sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (folder.events.size == 1) "1 event" else "${folder.events.size} events",
                        style = sheetMono(12.sp, color = HomeInkSecondary)
                    )
                    Spacer(Modifier.weight(1f))
                    Box {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HomeBackground)
                                .clickable { menuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "Folder options", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Rename folder") }, onClick = { menuOpen = false; renaming = true })
                            DropdownMenuItem(text = { Text("Ungroup") }, onClick = { menuOpen = false; onUngroup() })
                        }
                    }
                }

                TileGrid(folder.events + listOf(null), columns = 3) { tile, modifier ->
                    if (tile == null) {
                        AddHereTile(modifier, onAddHere)
                    } else {
                        val isJust = justLogged?.typeId == tile.typeId
                        EventTileView(
                            tile = tile,
                            now = now,
                            modifier = modifier,
                            editingPins = editingPins,
                            pinned = tile.typeId in pinnedIds,
                            onClick = { onTile(tile) },
                            onLongClick = { onTileLongPress(tile) },
                            height = 96.dp,
                            sub = when {
                                tile.runningSince != null -> formatElapsedCompact(now - tile.runningSince)
                                isJust -> "logged ${formatClock(justLogged!!.time)}"
                                else -> tile.todayLabel
                            },
                            highlighted = isJust,
                            tint = 0.1f
                        )
                    }
                }

                Text(
                    "Tap to log. Hold a tile to log it at a different time. Use ••• to rename or ungroup this folder.",
                    style = sheetText(12.sp, color = HomeInkSecondary, lineHeight = 17.sp)
                )
            }

            if (toast != null) {
                SheetToastBar(
                    toast = toast,
                    onUndo = onUndo,
                    onPause = onPause,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
                )
            }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename folder") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { renaming = false; onRename(name) }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddHereTile(modifier: Modifier, onClick: () -> Unit) {
    val dash = SheetHandle
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                )
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text("Add here", style = sheetText(13.sp, FontWeight.SemiBold, HomeInkSecondary))
    }
}
