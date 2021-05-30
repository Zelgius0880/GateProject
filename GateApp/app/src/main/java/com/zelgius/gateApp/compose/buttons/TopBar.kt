package com.zelgius.gateApp.compose.buttons

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.R

@Composable
fun AppTopBar(signal: Int) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.gate_project)) },
        backgroundColor = MaterialTheme.colors.primary,
        actions = {
            Icon(
                painter = painterResource(
                    when (signal.coerceAtMost(4)) {
                        -1 -> R.drawable.twotone_signal_wifi_off_24
                        0 -> R.drawable.twotone_signal_wifi_0_bar_24
                        1 -> R.drawable.twotone_signal_wifi_1_bar_24
                        2 -> R.drawable.twotone_signal_wifi_2_bar_24
                        3 -> R.drawable.twotone_signal_wifi_3_bar_24
                        4 -> R.drawable.twotone_signal_wifi_4_bar_24
                        else -> error("Wrong signal strength: $signal")
                    },
                ),
                tint = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                contentDescription = null
            )
        })
}

@Preview
@Composable
fun AppTopBarPreview() {
    AppTopBar(signal = 4)
}

