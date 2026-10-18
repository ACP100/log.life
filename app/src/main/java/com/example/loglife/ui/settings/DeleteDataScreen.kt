package com.example.loglife.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.backup.BackupRepository
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.components.TileButton
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import java.text.NumberFormat

private const val CONFIRM_WORD = "DELETE"

@Composable
fun DeleteDataScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val backUp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupRepository.Format.JSON.mimeType)) { uri ->
        uri?.let { viewModel.export(it, BackupRepository.Format.JSON) }
    }
    DeleteDataContent(
        counts = counts,
        onBack = onBack,
        onBackUp = { backUp.launch(viewModel.suggestedFileName(BackupRepository.Format.JSON)) },
        onDelete = { viewModel.deleteEverything(onDeleted) }
    )
}

@Composable
fun DeleteDataContent(counts: DataCounts, onBack: () -> Unit, onBackUp: () -> Unit, onDelete: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    val armed = typed.trim() == CONFIRM_WORD
    val number = NumberFormat.getIntegerInstance()

    Box(
        Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .imePadding()
    ) {
        SettingsPage(
            title = "Delete all data?",
            backLabel = "Settings",
            onBack = onBack,
            subtitle = "This removes everything from this phone. It can’t be undone unless you have a backup.",
            modifier = Modifier.padding(bottom = 88.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(HomeSurface)
            ) {
                Row {
                    CountCell(number.format(counts.logs), "logs", Modifier.weight(1f))
                    VerticalRule()
                    CountCell(counts.events.toString(), "events, ${counts.archived} archived", Modifier.weight(1f))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(SheetDivider))
                Row {
                    CountCell(counts.categories.toString(), "categories", Modifier.weight(1f))
                    VerticalRule()
                    CountCell(counts.running.toString(), "running sessions", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            TileButton("Back up first", onBackUp, Modifier.fillMaxWidth(), icon = Icons.Filled.Download, background = HomeSurface)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type ", style = sheetText(15.sp, FontWeight.SemiBold))
                Text(
                    CONFIRM_WORD,
                    style = sheetMono(14.sp, FontWeight.Medium),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SheetMuted)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(" to confirm", style = sheetText(15.sp, FontWeight.SemiBold))
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HomeSurface)
                    .border(2.dp, HomeInk, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it.take(16) },
                    singleLine = true,
                    textStyle = sheetMono(18.sp, FontWeight.Medium),
                    cursorBrush = SolidColor(HomeInk),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Type DELETE to confirm" }
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (armed) HomeAccent else SheetMuted)
                .clickable(enabled = armed, onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Delete everything",
                style = sheetText(17.sp, FontWeight.SemiBold, if (armed) Color.White else HomeInkSecondary)
            )
        }
    }
}

@Composable
private fun CountCell(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(16.dp)) {
        Text(value, style = sheetMono(24.sp, FontWeight.Medium))
        Text(label, style = sheetText(13.sp, color = HomeInkSecondary))
    }
}

@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .width(1.dp)
            .height(76.dp)
            .background(SheetDivider)
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun DeleteDataPreview() {
    LoglifeTheme {
        DeleteDataContent(DataCounts(1842, 24, 3, 6, 2), onBack = {}, onBackUp = {}, onDelete = {})
    }
}
