package com.example.loglife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText

/*
 * Building blocks for the Settings and Widgets pages: a back pill, a big
 * title, uppercase section labels and white cards of rows.
 */

/**
 * Scrolling page with the title; [content] goes below it. With a
 * [backLabel], a back pill sits above the title.
 */
@Composable
fun SettingsPage(
    title: String,
    backLabel: String? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .background(HomeBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 32.dp)
    ) {
        if (backLabel != null) {
            BackPill(backLabel, onBack)
            Spacer(Modifier.height(16.dp))
        }
        Text(
            title,
            style = sheetText(32.sp, FontWeight.Bold, letterSpacing = (-0.6).sp, lineHeight = 36.sp),
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = sheetText(14.sp, color = HomeInkSecondary, lineHeight = 19.sp))
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun BackPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeSurface)
            .clickable(onClickLabel = "Back to $label", onClick = onClick)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = HomeInk, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(2.dp))
        Text(label, style = sheetText(15.sp, FontWeight.SemiBold))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(),
            style = sheetText(12.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.2.sp),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (trailing != null) Text(trailing, style = sheetMono(13.sp, color = HomeInkSecondary))
    }
}

/** White rounded card; put [CardRow]s (or anything) inside, separated with [CardDivider]. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeSurface),
        content = content
    )
}

@Composable
fun CardDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SheetDivider)
    )
}

/** Title + optional subtitle on the left, [trailing] on the right. */
@Composable
fun CardRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = HomeInk,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = sheetText(16.sp, FontWeight.SemiBold, titleColor))
            if (subtitle != null) {
                Text(subtitle, style = sheetText(13.sp, color = HomeInkSecondary, lineHeight = 17.sp))
            }
        }
        trailing()
    }
}

/** Row that opens another page: optional mono [value] and a chevron. */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null
) {
    CardRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        leading = icon?.let { { IconTile(it) } }
    ) {
        if (value != null) {
            Text(value, style = sheetMono(13.sp, color = HomeInkSecondary))
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun IconTile(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SheetMuted),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = HomeInk, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    CardRow(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Spacer(Modifier.width(12.dp))
        PillSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

/** Equal-width segmented choice, as in Theme / Clock / Week starts on. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SheetMuted)
            .padding(4.dp)
            .selectableGroup()
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(11.dp)) else Modifier)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) HomeSurface else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = sheetText(15.sp, if (on) FontWeight.Bold else FontWeight.Medium, if (on) HomeInk else HomeInkSecondary),
                    maxLines = 1
                )
            }
        }
    }
}

/** A titled block inside a card with a segmented control under it. */
@Composable
fun SegmentedBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    subtitle: String? = null,
    value: String? = null
) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = sheetText(16.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (value != null) Text(value, style = sheetMono(13.sp, color = HomeInkSecondary))
        }
        if (subtitle != null) Text(subtitle, style = sheetText(13.sp, color = HomeInkSecondary, lineHeight = 17.sp))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** One choice of a radio list inside a card. */
@Composable
fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit, subtitle: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioMark(selected)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = sheetText(16.sp, FontWeight.SemiBold))
            if (subtitle != null) Text(subtitle, style = sheetText(13.sp, color = HomeInkSecondary, lineHeight = 17.sp))
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    if (selected) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(HomeInk),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(HomeSurface),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(HomeInk)
                )
            }
        }
    } else {
        Box(
            Modifier
                .size(22.dp)
                .border(2.dp, HomeInkMuted, CircleShape)
        )
    }
}

/** − value + stepper, as in "After running for 4 h" and "Recent rows 3". */
@Composable
fun Stepper(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    label: String,
    format: (Int) -> String = { it.toString() }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = label
            stateDescription = format(value)
        }
    ) {
        StepButton(Icons.Filled.Remove, "Decrease $label", enabled = value > range.first) { onChange(value - 1) }
        Text(
            format(value),
            style = sheetMono(18.sp, FontWeight.Medium),
            modifier = Modifier
                .width(64.dp)
                .padding(horizontal = 4.dp),
            textAlign = TextAlign.Center
        )
        StepButton(Icons.Filled.Add, "Increase $label", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SheetMuted)
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = HomeInk, modifier = Modifier.size(20.dp))
    }
}

/** Grey tile button (Export CSV / JSON, Refresh all widgets, Back up first). */
@Composable
fun TileButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color = SheetMuted,
    enabled: Boolean = true
) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = HomeInk, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = sheetText(15.sp, FontWeight.SemiBold))
    }
}
