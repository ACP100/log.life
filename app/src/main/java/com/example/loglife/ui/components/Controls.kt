package com.example.loglife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetSwitchOff

/** Filled dot for Sessions, hollow ring for Moments. */
@Composable
fun EventMarker(color: Color, isSession: Boolean, size: Dp = 12.dp) {
    Box(
        Modifier
            .size(size)
            .then(
                if (isSession) Modifier.clip(CircleShape).background(color)
                else Modifier.border(2.dp, color, CircleShape)
            )
    )
}

@Composable
fun PillSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) HomeInk else SheetSwitchOff)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(26.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(HomeSurface)
        )
    }
}
