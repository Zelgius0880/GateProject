package com.zelgius.gateApp.compose.buttons

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.R
import com.zelgius.gateApp.compose.DarkColors
import com.zelgius.gateApp.compose.LightColors

private object PrimaryForceStatusButtonRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color =
        RippleTheme.defaultRippleColor(MaterialTheme.colors.primaryVariant, !isSystemInDarkTheme())

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
        MaterialTheme.colors.primaryVariant,
        lightTheme = !isSystemInDarkTheme()
    ).let {
        RippleAlpha(
            draggedAlpha = it.draggedAlpha,
            focusedAlpha = it.focusedAlpha,
            hoveredAlpha = it.hoveredAlpha,
            pressedAlpha = if (isSystemInDarkTheme()) 0.2f else 0.7f
        )
    }
}

private object SecondaryForceStatusButtonRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color =
        RippleTheme.defaultRippleColor(
            MaterialTheme.colors.secondaryVariant,
            !isSystemInDarkTheme()
        )

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
        MaterialTheme.colors.secondaryVariant,
        lightTheme = !isSystemInDarkTheme()
    ).let {
        RippleAlpha(
            draggedAlpha = it.draggedAlpha,
            focusedAlpha = it.focusedAlpha,
            hoveredAlpha = it.hoveredAlpha,
            pressedAlpha = if (isSystemInDarkTheme()) 0.2f else 0.7f
        )
    }
}

@Composable
fun ForceStatusButton(
    currentStatus: GateStatus,
    modifier: Modifier = Modifier,
    onStateClicked: (GateStatus) -> Unit
) {
    val firstItemColor by animateColorAsState(
        targetValue = if (currentStatus == GateStatus.OPENED) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface
    )

    val firstItemTextColor by animateColorAsState(
        targetValue = if (currentStatus == GateStatus.OPENED) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
    )

    val secondItemColor by animateColorAsState(
        if (currentStatus == GateStatus.NOT_WORKING)
            if (isSystemInDarkTheme()) Color(0xffb1bfca) else Color(0xffe3f2fd)
        else MaterialTheme.colors.surface
    )

    val thirdItemColor by animateColorAsState(
        targetValue = if (currentStatus == GateStatus.CLOSED) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.surface
    )

    val thirdItemTextColor by animateColorAsState(
        targetValue = if (currentStatus == GateStatus.CLOSED) MaterialTheme.colors.onSecondary else MaterialTheme.colors.onSurface
    )

    Row(modifier) {
        CompositionLocalProvider(LocalRippleTheme provides PrimaryForceStatusButtonRippleTheme) {
            RoundedCornerText(
                text = stringResource(id = R.string.opened),
                backgroundColor = firstItemColor,
                textColor = firstItemTextColor,
                borderColor = MaterialTheme.colors.onSurface,
                shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
            ) {
                onStateClicked(GateStatus.OPENED)
            }
        }

        val border = 1.dp
        val borderColor = MaterialTheme.colors.onSurface
        Text(
            text = " - ",
            Modifier
                .background(secondItemColor)
                .drawBehind {
                    val strokeWidth = border.value * density
                    val y = size.height - strokeWidth / 2

                    drawLine(
                        borderColor,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth
                    )

                    drawLine(
                        borderColor,
                        Offset(0f, strokeWidth / 2),
                        Offset(size.width, strokeWidth / 2),
                        strokeWidth
                    )
                }
                .padding(vertical = 8.dp, horizontal = 8.dp)
            ,
            color = MaterialTheme.colors.onSurface
        )

        CompositionLocalProvider(LocalRippleTheme provides SecondaryForceStatusButtonRippleTheme) {
            RoundedCornerText(
                text = stringResource(id = R.string.closed),
                backgroundColor = thirdItemColor,
                textColor = thirdItemTextColor,
                borderColor = MaterialTheme.colors.onSurface,
                shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
            ) {
                onStateClicked(GateStatus.CLOSED)
            }
        }
    }
}

@Composable
fun RoundedCornerText(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    onClick: () -> Unit = {}
) {
    Text(
        text = text,
        modifier
            .ifNotNull(shape) {
                clip(it)
            }
            .background(backgroundColor)
            .ifNotNull(shape) {
                border(
                    1.dp,
                    borderColor,
                    it
                )
            }
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        color = textColor,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun ForceStatusButtonPreview() {
    Preview(isDark = isSystemInDarkTheme())
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ForceStatusButtonPreviewDark() {
    Preview(isDark = isSystemInDarkTheme())
}

@Composable
fun Preview(isDark: Boolean) {
    var status by remember {
        mutableStateOf(GateStatus.NOT_WORKING)
    }

    Column {
        MaterialTheme(colors = if (isDark) DarkColors else LightColors) {
            ForceStatusButton(currentStatus = status, onStateClicked = { status = it })
        }
    }
}


inline fun <T : Any> Modifier.ifNotNull(value: T?, builder: Modifier.(T) -> Modifier): Modifier =
    then(if (value != null) builder(value) else Modifier)