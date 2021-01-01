package com.zelgius.gateApp.compose.buttons

import androidx.annotation.IntRange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.R

@Composable
fun CardStatus(
    @IntRange(from = -1, to = 4) signalStrength: Int,
    @IntRange(from = 0, to = 100) progress: Int,
    action: GateWork,
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
                        when (signalStrength) {
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
                            GateWork.DOING_NOTHING -> R.string.not_working
                            GateWork.OPENING -> R.string.opening
                            GateWork.CLOSING -> R.string.closing
                        }
                    )
                }...", modifier = Modifier.padding(top = 16.dp)
            )
            LinearProgressIndicator(
                progress = if (action == GateWork.DOING_NOTHING) 0f else progress / 100f,
                modifier = Modifier.padding(top = 8.dp),
                color = when (action) {
                    GateWork.DOING_NOTHING -> MaterialTheme.colors.primary
                    GateWork.OPENING -> MaterialTheme.colors.primary
                    GateWork.CLOSING -> MaterialTheme.colors.secondary
                }
            )
        }
    }
}

@Preview
@Composable
fun CardStatusPreview() {
    CardStatus(3, 50, GateWork.OPENING)
}

enum class GateWork {
    OPENING, CLOSING, DOING_NOTHING
}