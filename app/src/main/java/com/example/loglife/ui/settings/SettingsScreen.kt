package com.example.loglife.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.backup.BackupRepository
import com.example.loglife.data.settings.AppSettings
import com.example.loglife.data.settings.ConcurrentPolicy
import com.example.loglife.data.settings.ThemeMode
import com.example.loglife.data.settings.WeekStart
import com.example.loglife.ui.components.CardDivider
import com.example.loglife.ui.components.CardRow
import com.example.loglife.ui.components.NavRow
import com.example.loglife.ui.components.RadioRow
import com.example.loglife.ui.components.SectionLabel
import com.example.loglife.ui.components.Segmented
import com.example.loglife.ui.components.SegmentedBlock
import com.example.loglife.ui.components.SettingsCard
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.components.Stepper
import com.example.loglife.ui.components.TileButton
import com.example.loglife.ui.components.ToggleRow
import com.example.loglife.ui.home.HomeSnackbar
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.widgets.WidgetPlacement
import com.example.loglife.util.formatClock
import java.text.NumberFormat

@Composable
fun SettingsScreen(
    onOpenEvents: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenDeleteData: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Widgets can be added or removed on the home screen while we're away.
    var placedWidgets by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        placedWidgets = WidgetPlacement.placedCount(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupRepository.Format.CSV.mimeType)) { uri ->
        uri?.let { viewModel.export(it, BackupRepository.Format.CSV) }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupRepository.Format.JSON.mimeType)) { uri ->
        uri?.let { viewModel.export(it, BackupRepository.Format.JSON) }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.pickRestore(it, displayName(context, it)) }
    }

    // Turning on a notification setting asks for the permission first (Android 13+).
    var pendingNotificationChange by remember { mutableStateOf<((AppSettings) -> AppSettings)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val change = pendingNotificationChange
        pendingNotificationChange = null
        if (granted && change != null) viewModel.update(change)
    }
    fun enableNotifications(change: (AppSettings) -> AppSettings) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingNotificationChange = change
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.update(change)
        }
    }

    var showLicences by rememberSaveable { mutableStateOf(false) }

    Box {
        SettingsContent(
            settings = settings,
            counts = counts,
            placedWidgets = placedWidgets,
            versionName = remember { versionName(context) },
            onUpdate = viewModel::update,
            onEnableNotifications = ::enableNotifications,
            onOpenEvents = onOpenEvents,
            onOpenWidgets = onOpenWidgets,
            onExportCsv = { exportCsv.launch(viewModel.suggestedFileName(BackupRepository.Format.CSV)) },
            onExportJson = { exportJson.launch(viewModel.suggestedFileName(BackupRepository.Format.JSON)) },
            onRestore = { restore.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            onDeleteAll = onOpenDeleteData,
            onLicences = { showLicences = true },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp)) { data ->
            HomeSnackbar(message = data.visuals.message, actionLabel = null, onAction = {})
        }
    }

    pendingRestore?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "${pending.fileLabel} has ${pending.data.logs.size} logs and ${pending.data.eventTypes.size} events. " +
                        "Restoring replaces everything on this phone (${counts.logs} logs, ${counts.events} events)."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("Replace", color = HomeAccent) } },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancel") } }
        )
    }

    if (showLicences) LicencesDialog(onDismiss = { showLicences = false })
}

/** ViewModel-free so it renders in Preview. */
@Composable
fun SettingsContent(
    settings: AppSettings,
    counts: DataCounts,
    placedWidgets: Int,
    versionName: String,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onEnableNotifications: ((AppSettings) -> AppSettings) -> Unit,
    onOpenEvents: () -> Unit,
    onOpenWidgets: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onRestore: () -> Unit,
    onDeleteAll: () -> Unit,
    onLicences: () -> Unit,
) {
    val number = remember { NumberFormat.getIntegerInstance() }
    SettingsPage(title = "Settings") {
        SectionLabel("Display")
        SettingsCard {
            SegmentedBlock("Theme", content = {
                Segmented(
                    options = ThemeMode.entries,
                    selected = settings.theme,
                    label = { it.label },
                    onSelect = { mode -> onUpdate { it.copy(theme = mode) } },
                    description = "Theme"
                )
            })
            CardDivider()
            SegmentedBlock("Clock", value = "since ${formatClock(System.currentTimeMillis())}", content = {
                Segmented(
                    options = listOf(true, false),
                    selected = settings.clock24h,
                    label = { if (it) "24-hour" else "12-hour" },
                    onSelect = { is24 -> onUpdate { it.copy(clock24h = is24) } },
                    description = "Clock"
                )
            })
            CardDivider()
            SegmentedBlock("Week starts on", subtitle = "Used by History and the week totals", content = {
                Segmented(
                    options = WeekStart.entries,
                    selected = settings.weekStart,
                    label = { it.label },
                    onSelect = { day -> onUpdate { it.copy(weekStart = day) } },
                    description = "Week starts on"
                )
            })
        }

        SectionLabel("Logging")
        SettingsCard {
            Text(
                "Starting a session while another runs",
                style = sheetText(16.sp, FontWeight.SemiBold),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            )
            ConcurrentPolicy.entries.forEach { policy ->
                CardDivider()
                RadioRow(
                    title = policy.title,
                    subtitle = policy.subtitle,
                    selected = settings.concurrent == policy,
                    onClick = { onUpdate { it.copy(concurrent = policy) } }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            NavRow(
                title = "Events & categories",
                subtitle = "Rename, recolour, archive, restore",
                value = "${counts.events} · ${counts.categories}",
                icon = Icons.Outlined.Category,
                onClick = onOpenEvents
            )
            CardDivider()
            NavRow(
                title = "Widgets",
                subtitle = "Favourites, taps, look",
                value = "$placedWidgets placed",
                icon = Icons.Outlined.Widgets,
                onClick = onOpenWidgets
            )
        }

        SectionLabel("Notifications")
        SettingsCard {
            ToggleRow(
                title = "While a session runs",
                subtitle = "Ongoing notification with a timer and Stop",
                checked = settings.ongoingNotification,
                onCheckedChange = { on ->
                    if (on) onEnableNotifications { it.copy(ongoingNotification = true) }
                    else onUpdate { it.copy(ongoingNotification = false) }
                }
            )
            CardDivider()
            ToggleRow(
                title = "Long-running reminder",
                subtitle = "Nudge me if I forgot to stop",
                checked = settings.reminderEnabled,
                onCheckedChange = { on ->
                    if (on) onEnableNotifications { it.copy(reminderEnabled = true) }
                    else onUpdate { it.copy(reminderEnabled = false) }
                }
            )
            if (settings.reminderEnabled) {
                CardDivider()
                CardRow(title = "After running for", titleColor = HomeInkSecondary) {
                    Stepper(
                        value = settings.reminderHours,
                        range = AppSettings.REMINDER_HOURS,
                        onChange = { h -> onUpdate { it.copy(reminderHours = h) } },
                        label = "Reminder after",
                        format = { "$it h" }
                    )
                }
            }
        }

        SectionLabel("Data")
        SettingsCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Export", style = sheetText(16.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    Text("${number.format(counts.logs)} logs", style = sheetMono(13.sp, color = HomeInkSecondary))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TileButton("CSV", onExportCsv, Modifier.weight(1f), icon = Icons.Filled.Download)
                    TileButton("JSON", onExportJson, Modifier.weight(1f), icon = Icons.Filled.Download)
                }
            }
            CardDivider()
            NavRow(title = "Restore from a backup", subtitle = "Replaces what’s on this phone", onClick = onRestore)
            CardDivider()
            Text(
                "Delete all data…",
                style = sheetText(16.sp, FontWeight.SemiBold, HomeAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onDeleteAll)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            )
        }

        SectionLabel("About")
        SettingsCard {
            CardRow(title = "Version") {
                Text(versionName, style = sheetMono(14.sp, color = HomeInkSecondary))
            }
            CardDivider()
            NavRow(title = "Open-source licences", onClick = onLicences)
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

private val WeekStart.label: String
    get() = when (this) {
        WeekStart.PHONE -> "Phone"
        WeekStart.MONDAY -> "Mon"
        WeekStart.SATURDAY -> "Sat"
        WeekStart.SUNDAY -> "Sun"
    }

private val ConcurrentPolicy.title: String
    get() = when (this) {
        ConcurrentPolicy.ASK -> "Ask each time"
        ConcurrentPolicy.KEEP_BOTH -> "Keep both running"
        ConcurrentPolicy.STOP_OTHER -> "Stop the other one"
    }

private val ConcurrentPolicy.subtitle: String
    get() = when (this) {
        ConcurrentPolicy.ASK -> "Show Keep both / Stop the other"
        ConcurrentPolicy.KEEP_BOTH -> "Sessions can overlap"
        ConcurrentPolicy.STOP_OTHER -> "Only one session at a time"
    }

private fun versionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "–"
} catch (_: Exception) {
    "–"
}

private fun displayName(context: Context, uri: Uri): String = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: "This file"
} catch (_: Exception) {
    "This file"
}

private val licences = listOf(
    "AndroidX (Compose, Room, Lifecycle, WorkManager, Navigation)" to "Apache License 2.0",
    "Dagger & Hilt" to "Apache License 2.0",
    "Kotlin & kotlinx.coroutines" to "Apache License 2.0",
    "Material Components for Android" to "Apache License 2.0",
    "Material Symbols & Icons" to "Apache License 2.0",
    "Figtree typeface" to "SIL Open Font License 1.1",
    "JetBrains Mono typeface" to "SIL Open Font License 1.1",
)

@Composable
private fun LicencesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open-source licences") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                licences.forEach { (name, licence) ->
                    Column {
                        Text(name, style = sheetText(15.sp, FontWeight.SemiBold))
                        Text(licence, style = sheetText(13.sp, color = HomeInkSecondary))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1500)
@Composable
private fun SettingsContentPreview() {
    LoglifeTheme {
        SettingsContent(
            settings = AppSettings(reminderEnabled = true),
            counts = DataCounts(logs = 1842, events = 24, archived = 3, categories = 6, running = 2),
            placedWidgets = 4,
            versionName = "1.0",
            onUpdate = {}, onEnableNotifications = {}, onOpenEvents = {}, onOpenWidgets = {},
            onExportCsv = {}, onExportJson = {}, onRestore = {}, onDeleteAll = {}, onLicences = {},
        )
    }
}
