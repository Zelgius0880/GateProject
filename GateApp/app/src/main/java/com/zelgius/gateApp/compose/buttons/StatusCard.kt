package com.zelgius.gateApp.compose.buttons

import androidx.annotation.IntRange
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.R

@Composable
fun CardStatus(
    @IntRange(from = -1, to = 4) signalStrength: Int,
    @IntRange(from = 0, to = 100) progress: Int,
    action: GateStatus,
    modifier: Modifier = Modifier
) {
    Card(modifier = Modifier.padding(8.dp) then modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Box(modifier = Modifier.align(Alignment.Bottom)) {
                    Text(text = stringResource(id = R.string.gate_signal))
                }
                Icon(
                    imageVector = vectorResource(
                        when (signalStrength.coerceAtMost(4)) {
                            -1 -> R.drawable.twotone_signal_wifi_off_24
                            0 -> R.drawable.twotone_signal_wifi_0_bar_24
                            1 -> R.drawable.twotone_signal_wifi_1_bar_24
                            2 -> R.drawable.twotone_signal_wifi_2_bar_24
                            3 -> R.drawable.twotone_signal_wifi_3_bar_24
                            4 -> R.drawable.twotone_signal_wifi_4_bar_24
                            else -> error("Wrong signal strength: $signalStrength")
                        }
                    ),
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = "${
                    stringResource(
                        id =
                        when (action) {
                            GateStatus.NOT_WORKING -> R.string.not_working
                            GateStatus.OPENED-> R.string.opened
                            GateStatus.OPENING -> R.string.opening
                            GateStatus.CLOSING -> R.string.closing
                            GateStatus.CLOSED -> R.string.closed
                        }
                    )
                }...", modifier = Modifier.padding(top = 16.dp)
            )
            LinearProgressIndicator(
                progress = if (action == GateStatus.NOT_WORKING) 0f else progress / 100f,
                modifier = Modifier.padding(top = 8.dp) then Modifier.fillMaxWidth(),
                color = when (action) {
                    GateStatus.NOT_WORKING -> MaterialTheme.colors.primary
                    GateStatus.OPENING, GateStatus.OPENED -> MaterialTheme.colors.primary
                    GateStatus.CLOSING, GateStatus.CLOSED -> MaterialTheme.colors.secondary
                }
            )
        }
    }
}

@Preview
@Composable
fun CardStatusPreview() {
    CardStatus(3, 50, GateStatus.OPENING)
}