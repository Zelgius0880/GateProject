package com.zelgius.gateApp.compose.buttons

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.R
import com.zelgius.gateApp.WifiViewModel
import java.util.*


@Composable
fun StepOpenButton(
    wifiViewModel: WifiViewModel?,
    modifier: Modifier = Modifier
) { // nullable for preview
    TextButton(
        onClick = {
            wifiViewModel?.stepOpenGate()
        },
        enabled = wifiViewModel?.working != true,
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.open_a_bit),
            color = MaterialTheme.colors.primaryVariant
        )
    }
}

@Composable
fun StepCloseButton(
    wifiViewModel: WifiViewModel?,
    modifier: Modifier = Modifier
) { // nullable for preview
    TextButton(
        onClick = {
            wifiViewModel?.stepCloseGate()
        },
        enabled = wifiViewModel?.working != true,
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.close_a_bit),
            color = MaterialTheme.colors.secondaryVariant
        )
    }
}

@Composable
fun OpenButton(
    wifiViewModel: WifiViewModel?,
    modifier: Modifier = Modifier
) { // nullable for preview
    val enabled =
        wifiViewModel?.status != GateStatus.OPENED && wifiViewModel?.status != GateStatus.OPENING
    Button(
        onClick = {
            wifiViewModel?.openGate()
        },
        enabled = wifiViewModel?.status != GateStatus.OPENED && wifiViewModel?.status != GateStatus.OPENING,
        modifier = modifier,
        colors = ButtonConstants.defaultButtonColors(MaterialTheme.colors.primaryVariant)
    ) {

        Text(
            text = stringResource(id = R.string.open).toUpperCase(Locale.ROOT),
            color = MaterialTheme.colors.onPrimary
        )

    }
}

@Composable
fun CloseButton(
    wifiViewModel: WifiViewModel?,
    modifier: Modifier = Modifier
) { // nullable for preview
    val enabled =
        wifiViewModel?.status != GateStatus.CLOSING && wifiViewModel?.status != GateStatus.CLOSED
    Button(
        onClick = {
            wifiViewModel?.closeGate()
        },
        enabled = enabled,
        modifier = modifier,
        colors = ButtonConstants.defaultButtonColors(MaterialTheme.colors.secondaryVariant)
    ) {

        Text(
            text = stringResource(id = R.string.close).toUpperCase(Locale.ROOT),
            color = MaterialTheme.colors.onSecondary
        )
    }
}

@Composable
fun CardOpenClose(viewModel: WifiViewModel?, modifier: Modifier = Modifier) {
    Card(modifier = Modifier.padding(8.dp) then modifier) {
        Column(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {

                OpenButton(
                    wifiViewModel = viewModel,
                    modifier = Modifier.padding(end = 4.dp) then Modifier.weight(1f)
                )

                CloseButton(
                    wifiViewModel = viewModel,
                    modifier = Modifier.padding(start = 4.dp) then Modifier.weight(1f)
                )

            }

        }
    }
}

@Composable
fun CardConfiguration(
    wifiViewModel: WifiViewModel?, modifier: Modifier = Modifier
) {// nullable for preview

    Card(modifier = Modifier.padding(8.dp) then modifier) {
        Column(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.configuration),
                style = MaterialTheme.typography.h5
            )
            if (wifiViewModel?.connected != true) {
                Providers(AmbientContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = stringResource(id = R.string.need_to_be_connected),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }

            if (wifiViewModel?.connected != false) {
                StepButtons(wifiViewModel)
                StartConfiguration(viewModel = wifiViewModel)
            }
        }
    }
}


@Composable
fun StepButtons(wifiViewModel: WifiViewModel?) {
    Text(
        text = stringResource(id = R.string.step_opening),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.subtitle1
    )
    Row(modifier = Modifier.padding(8.dp) then Modifier.fillMaxWidth()) {
        StepOpenButton(
            wifiViewModel = wifiViewModel,
            modifier = Modifier.padding(end = 4.dp) then Modifier.weight(1f)
        )
        StepCloseButton(
            wifiViewModel = wifiViewModel,
            modifier = Modifier.padding(start = 4.dp) then Modifier.weight(1f)
        )
    }
}


@Composable
fun StartConfiguration(viewModel: WifiViewModel?) {
    Column {
        if (viewModel?.status != GateStatus.CLOSED && viewModel?.config != true) {
            Providers(AmbientContentColor provides MaterialTheme.colors.error) {
                Text(
                    text = stringResource(id = R.string.gate_not_closed),
                    style = MaterialTheme.typography.subtitle1
                )

                Button(onClick = { viewModel?.forceClose() }) {
                    Text(text = stringResource(id = R.string.it_is_closed).toUpperCase(Locale.ROOT))
                }
            }
        } else {
            Button(onClick = {
                if (!viewModel.config)
                    viewModel.startConfig()
                else
                    viewModel.stopConfig()
            }) {
                Text(
                    text = stringResource(id = if (viewModel.config) R.string.start else R.string.stop).toUpperCase(
                        Locale.ROOT
                    )
                )
            }
        }
    }
}

@Composable
fun CardTime(viewModel: WifiViewModel?, modifier: Modifier = Modifier) {
    var edit by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("${viewModel?.time ?: 0L}") }
    Card(modifier = Modifier.padding(8.dp) then modifier) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!edit) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        id = R.string.movement_time_format,
                        viewModel?.time ?: 0
                    )
                )

                IconButton(onClick = { edit = true }) {
                    Icon(
                        imageVector = vectorResource(
                            R.drawable.ic_baseline_edit_24
                        ), tint = MaterialTheme.colors.primary
                    )
                }
            } else {
                Text(
                    text = "${
                        stringResource(
                            id = R.string.movement_time,
                            viewModel?.time ?: 0
                        )
                    }:"
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                )

                Text(text = "ms")

                IconButton(
                    onClick = {
                        edit = false
                    },
                ) {
                    Icon(
                        imageVector = vectorResource(
                            R.drawable.ic_twotone_close_24
                        ), tint = MaterialTheme.colors.secondary
                    )
                }

                IconButton(onClick = {
                    edit = false
                    viewModel?.setMovingTime(text.toLong())
                }) {
                    Icon(
                        imageVector = vectorResource(
                            R.drawable.ic_twotone_check_24
                        ), tint = MaterialTheme.colors.primary
                    )
                }
            }

        }
    }
}

@Composable
@Preview
fun TimeCardPreview() {
    CardTime(null, Modifier.padding(8.dp))
}


