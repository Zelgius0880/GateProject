@file:OptIn(ExperimentalFoundationApi::class)

package com.zelgius.gateApp.compose

import android.view.MotionEvent
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.twotone.FavoriteBorder
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.GateSide
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.GateViewModel
import com.zelgius.gateApp.R
import com.zelgius.gateApp.compose.buttons.CloseButton
import com.zelgius.gateApp.compose.buttons.OpenButton
import com.zelgius.gateApp.compose.buttons.Time

@Composable
fun CardSide(side: GateSide, viewModel: GateViewModel, modifier: Modifier = Modifier) {

    Card(modifier = Modifier.padding(8.dp) then modifier) {
        Column {

            val status = when (side) {
                GateSide.Left -> viewModel.leftStatus
                GateSide.Right -> viewModel.rightStatus
            }.observeAsState(initial = GateStatus.NOT_WORKING)

            GateSideStatus(side, status)

            GateControl(
                status,
                onOpen = { viewModel.openGate(side) },
                onClose = { viewModel.closeGate(side) },
                onStop = { viewModel.stop(side) }
            )


            GateProgress(
                when (side) {
                    GateSide.Left -> viewModel.leftProgress
                    GateSide.Right -> viewModel.rightProgress
                }.observeAsState(initial = null), status
            )

            val time by when (side) {
                GateSide.Left -> viewModel.timeLeft
                GateSide.Right -> viewModel.timeRight
            }.observeAsState(0)

            val favorite by viewModel.favorite.observeAsState()

            Settings(time = time,
                isFavorite = favorite == side,
                onUpdate = {
                    viewModel.setMovingTime(side, it)
                },
                onFavoriteClicked = {
                    viewModel.toggleFavorite(side)
                },
                openPressChanged = {
                    if (!it)
                        viewModel.stopManual(side)
                    else
                        viewModel.manualOpenGate(side)
                },
                closePressChanged = {
                    if (!it)
                        viewModel.stopManual(side)
                    else
                        viewModel.manualCloseGate(side)
                }
            )

        }
    }
}

@Composable
private fun ColumnScope.GateProgress(
    progressState: State<Int?>,
    statusState: State<GateStatus>
) {
    val progress by progressState
    val status by statusState
    AnimatedVisibility(visible = progress != null) {
        LinearProgressIndicator(
            color = if (status == GateStatus.CLOSING) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.primaryVariant,
            progress = (progress ?: 0) / 100f,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .rotate(if (status == GateStatus.OPENING) 0f else 180f)
        )
    }
}

@Composable
private fun GateControl(
    statusState: State<GateStatus>,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onStop: () -> Unit,
) {
    val status by statusState
    Row(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {

        OpenButton(
            status = status,
            modifier = Modifier.padding(end = 4.dp) then Modifier.weight(1f),
            onClick = onOpen,
            onStop = onStop
        )

        CloseButton(
            status = status,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f),
            onClick = onClose,
            onStop = onStop
        )

    }
}

@Composable
private fun GateSideStatus(side: GateSide, statusState: State<GateStatus>) {
    Row(modifier = Modifier.padding(8.dp)) {
        Text(
            modifier = Modifier.weight(1f),
            text = when (side) {
                GateSide.Left -> stringResource(id = R.string.left_side)
                GateSide.Right -> stringResource(id = R.string.right_side)
            },
            style = MaterialTheme.typography.h6
        )

        val status by statusState
        Text(
            text = "${
                stringResource(
                    id =
                    when (status) {
                        GateStatus.NOT_WORKING -> R.string.not_working
                        GateStatus.OPENED -> R.string.opened
                        GateStatus.OPENING, GateStatus.MANUAL_OPENING -> R.string.opening
                        GateStatus.CLOSING, GateStatus.MANUAL_CLOSING -> R.string.closing
                        GateStatus.CLOSED -> R.string.closed
                    }
                )
            }...",
            modifier = Modifier
                .align(Alignment.Top)
                .padding(end = 8.dp)
        )
    }
}

@Composable
@Preview
fun CardSidePreview() {
    MaterialTheme(colors = LightColors) {
        CardSide(side = GateSide.Left, viewModel = GateViewModel(LocalContext.current))
    }
}


/**
 *
 */

@Composable
fun Settings(
    time: Long,
    isFavorite: Boolean,
    onUpdate: (time: Long) -> Unit,
    onFavoriteClicked: () -> Unit,
    openPressChanged: (isPressed: Boolean) -> Unit,
    closePressChanged: (isPressed: Boolean) -> Unit,
    isOpened: Boolean = false
) {
    var isPanelOpened by remember { mutableStateOf(isOpened) }
    val rotation by animateFloatAsState(targetValue = if (isPanelOpened) 60f else 0f)
    Column(modifier = Modifier.animateContentSize()) {
        Divider(
            color = MaterialTheme.colors.background,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )

        Row {
            Crossfade(
                targetState = isPanelOpened,
                modifier = Modifier.weight(1f),
                animationSpec = tween(150)
            ) {
                if (it)
                    Text(
                        text = stringResource(id = R.string.settings),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(8.dp)
                    )
            }

            val showTooltip = remember { mutableStateOf(false) }


            Box(modifier = Modifier.padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            role = Role.Button,
                            onClick = onFavoriteClicked,
                            onLongClick = { showTooltip.value = true },
                        )
                        .size(40.dp), contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = isFavorite,
                    ) {
                        if (it) {
                            Icon(
                                Icons.Filled.Favorite,
                                tint = MaterialTheme.colors.secondary,
                                contentDescription = "",
                            )

                        } else {
                            Icon(
                                Icons.TwoTone.FavoriteBorder,
                                tint = MaterialTheme.colors.secondary,
                                contentDescription = "",
                            )
                        }
                    }
                }

                MaterialTheme(if (!isSystemInDarkTheme()) DarkColors else LightColors) {
                    Tooltip(showTooltip, timeoutMillis = 3000L, offset =  DpOffset(0.dp, (-16).dp), backgroundColor = MaterialTheme.colors.surface) {
                        Text(stringResource(id = R.string.favorite_tooltip))
                    }
                }

            }

            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
                onClick = {
                    isPanelOpened = !isPanelOpened
                }) {
                Icon(
                    Icons.TwoTone.Settings,
                    contentDescription = "",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }


        if (isPanelOpened) {
            Time(time = time, onTimeUpdated = onUpdate)
            ManualOpening(
                openPressChanged = openPressChanged,
                closePressChanged = closePressChanged
            )
        }
    }
}


@Composable
fun ManualOpening(
    openPressChanged: (isPressed: Boolean) -> Unit,
    closePressChanged: (isPressed: Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = stringResource(id = R.string.manual_opening),
            style = MaterialTheme.typography.h6
        )

        Row(modifier = Modifier.padding(top = 8.dp)) {
            PressButton(
                text = stringResource(id = R.string.open),
                colors = MaterialTheme.colors,
                onPressChanged = openPressChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            PressButton(
                text = stringResource(id = R.string.close),
                colors = lightColors(
                    primary = MaterialTheme.colors.secondary,
                    onPrimary = MaterialTheme.colors.onSecondary,
                ),
                onPressChanged = closePressChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PressButton(
    text: String,
    colors: Colors,
    modifier: Modifier = Modifier,
    onPressChanged: (isPressed: Boolean) -> Unit,
) {
    var pressed by remember {
        mutableStateOf(false)
    }

    val unpressedTextColor = MaterialTheme.typography.button.color
    val background = remember { Animatable(colors.primary) }
    val textColor = remember { Animatable(unpressedTextColor) }

    LaunchedEffect(pressed) {
        background.animateTo(if (pressed) colors.primary else Color.Transparent)
        textColor.animateTo(if (pressed) colors.onPrimary else unpressedTextColor)
    }

    val shape = RoundedCornerShape(50)
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.button.copy(color = textColor.value),
        textAlign = TextAlign.Center,
        modifier = modifier

            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onPressChanged(true)
                        pressed = true
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        pressed = false
                        onPressChanged(false)
                        true
                    }
                    else -> false
                }
            }
            .clip(shape = shape)
            .let {
                if (!pressed) it.border(2.dp, color = colors.primary, shape = shape)
                else it.background(background.value)
            }
            .padding(horizontal = 8.dp, vertical = 16.dp)
    )
}

@Composable
@Preview
fun SettingsPreview() {
    Settings(
        500,
        isOpened = true,
        isFavorite = false,
        onUpdate = {},
        onFavoriteClicked = {},
        openPressChanged = {},
        closePressChanged = {})
}


