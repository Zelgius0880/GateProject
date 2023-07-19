package com.zelgius.gateApp.compose.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Light(isOn: Boolean, time: Long,  modifier: Modifier = Modifier, onTimeSet: (Long) -> Unit = {}, onToggle: (isOn: Boolean) -> Unit = {}) {
    val color by animateColorAsState(
        targetValue = if (isOn) Color(0xFFFFF176) else Color(0xFFFFFDE7),
        label = ""
    )
    var showDialog by remember {
        mutableStateOf(false)
    }

    LongPressButton(
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        modifier = modifier
            .size(125.dp),
        onClick = {
            onToggle(!isOn)
        },
        onLongClick = {
            showDialog = true
        }
    ) {
        Box {
            this@LongPressButton.AnimatedVisibility(
                visible = isOn,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.size(75.dp)
            ) {
                Image(Icons.Filled.Lightbulb, contentDescription = "")
            }

            this@LongPressButton.AnimatedVisibility(
                visible = !isOn,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.size(75.dp)
            ) {
                Image(Icons.Outlined.Lightbulb, contentDescription = "")
            }
        }
    }

    if (showDialog) {
        LightTimeDialog(
            time,
            onDismiss = { showDialog = false },
            onTimeSet = {
                showDialog = false
                onTimeSet(it)
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LongPressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionState: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    shape: CornerBasedShape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    Surface(
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = elevation?.elevation(enabled, interactionState)?.value ?: 0.dp,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick,
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionState,
            indication = null
        )
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth,
                            minHeight = ButtonDefaults.MinHeight
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Preview
@Composable
fun LightOnPreview() {
    var isOn by remember {
        mutableStateOf(true)
    }
    Light(isOn = isOn, 0L) {
        isOn = it
    }
}

@Preview
@Composable
fun LightOffPreview() {
    var isOn by remember {
        mutableStateOf(false)
    }
    Light(isOn = isOn, 0L) {
        isOn = it
    }
}