package com.example.loglife.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.util.formatClock
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeInkFill
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSnackbarAction
import com.example.loglife.ui.theme.JetBrainsMonoFamily
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.components.EventMarker

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text.uppercase(),
            style = sheetText(13.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp)
        )
        trailing()
    }
}

@Composable
internal fun SheetToastBar(
    toast: SheetToast,
    onUndo: (SheetToast.Logged) -> Unit,
    onPause: (SheetToast.AlsoRunning) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(HomeInkFill)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val onDark = Color(0xFFF3F3EF)
        when (toast) {
            is SheetToast.Logged -> {
                EventMarker(eventColor(toast.colorHex, toast.folder), toast.isSession, 10.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    buildAnnotatedString {
                        append(if (toast.started) "Started " else "Logged ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(toast.name) }
                        append(" · ")
                        withStyle(SpanStyle(fontFamily = JetBrainsMonoFamily)) { append(formatClock(toast.time)) }
                    },
                    style = sheetText(14.sp, FontWeight.Medium, onDark),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                ToastAction("Undo") { onUndo(toast) }
            }
            is SheetToast.AlsoRunning -> {
                Text(
                    "${toast.startedName} started. ${toast.otherName} keeps running.",
                    style = sheetText(13.sp, FontWeight.Medium, onDark),
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                ToastAction("Pause ${toast.otherName}") { onPause(toast) }
            }
        }
    }
}

@Composable
private fun ToastAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(14.sp, FontWeight.Bold, HomeSnackbarAction), maxLines = 1)
    }
}

@Composable
internal fun AccentButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(18.dp))
            .background(HomeAccent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(17.sp, FontWeight.SemiBold, Color.White))
    }
}

