package com.zelgius.gateApp.compose.buttons

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.R
import java.util.*

@Composable
fun OpenButton(
    status: GateStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onStop: () -> Unit
) {

    if (status != GateStatus.OPENING)
        OutlinedButton(
            onClick = onClick,
            enabled = status != GateStatus.OPENED,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.primaryVariant)
        ) {

            Text(
                text = stringResource(id = R.string.open).toUpperCase(Locale.ROOT),
            )

        }
    else
        TextButton(
            onClick = onStop,/*{
                wifiViewModel.stop()
            }*/
            modifier = modifier,
        ) {

            Text(
                text = stringResource(id = R.string.stop).toUpperCase(Locale.ROOT),
                color = MaterialTheme.colors.primary
            )

        }

}

@Composable
fun CloseButton(
    status: GateStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onStop: () -> Unit
) {
    if (status != GateStatus.CLOSING)
        OutlinedButton(
            onClick = onClick,
            enabled = status != GateStatus.CLOSED,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.secondaryVariant)
        ) {

            Text(
                text = stringResource(id = R.string.close).toUpperCase(Locale.ROOT),
            )
        }
    else
        TextButton(
            onClick = onStop,
            modifier = modifier
        ) {

            Text(
                text = stringResource(id = R.string.stop).toUpperCase(Locale.ROOT),
                color = MaterialTheme.colors.secondaryVariant
            )
        }
}

@Composable
fun CardOpenClose(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.padding(8.dp) then modifier) {
        Column(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {

                OpenButton(
                    status = GateStatus.NOT_WORKING,
                    modifier = Modifier.padding(end = 4.dp) then Modifier.weight(1f),
                    onClick = onOpen,
                    onStop = onStop
                )

                CloseButton(
                    status = GateStatus.NOT_WORKING,
                    modifier = Modifier.padding(start = 4.dp) then Modifier.weight(1f),
                    onClick = onClose,
                    onStop = onStop
                )

            }

        }
    }
}

@Composable
fun Time(time: Long, modifier: Modifier = Modifier, onTimeUpdated: (time: Long) -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("$time") }

    Crossfade(targetState = edit, modifier = modifier) {
        if (!it) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        id = R.string.movement_time_format,
                        time
                    )
                )

                IconButton(
                    onClick = { edit = true },
                ) {
                    Icon(
                        Icons.TwoTone.Edit,
                        tint = MaterialTheme.colors.primary,
                        contentDescription = null
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(
                    8.dp
                ), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${
                        stringResource(
                            id = R.string.movement_time,
                            time
                        )
                    }:",
                    modifier = Modifier.padding(end = 4.dp)
                )

                BasicTextField(
                    value = text,
                    onValueChange = { s -> text = s },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            BorderStroke(2.dp, color = MaterialTheme.colors.background),
                            RoundedCornerShape(10)
                        )
                        .padding(4.dp),
                )

                Text(text = "ms", modifier = Modifier.padding(start = 4.dp))

                IconButton(
                    onClick = {
                        edit = false
                        text = "$time"
                    },
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Close,
                        tint = MaterialTheme.colors.secondary,
                        contentDescription = null
                    )
                }

                IconButton(modifier = Modifier,
                    onClick = {
                        edit = false
                        text.toLongOrNull().let {
                            if (it == null)
                                text = "$time"
                            else
                                onTimeUpdated(it)
                        }
                    }) {
                    Icon(
                        imageVector = Icons.TwoTone.Check,
                        tint = MaterialTheme.colors.primary,
                        contentDescription = null
                    )
                }
            }

        }
    }
}

@Composable
@Preview
fun TimeCardPreview() {
    Time(500, Modifier.padding(8.dp)) {}
}


@Composable
@Preview
fun OpenButtonPreview() {
    OpenButton(GateStatus.NOT_WORKING, Modifier.padding(8.dp), onClick = {}, onStop = {})
}

@Composable
@Preview
fun CloseButtonPreview() {
    CloseButton(GateStatus.NOT_WORKING, Modifier.padding(8.dp), onClick = {}, onStop = {})
}


