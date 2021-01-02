package com.zelgius.gateApp.compose.buttons

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.zelgius.gateApp.R

@Composable
fun AppTopBar(connected: Boolean, onClick: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.gate_project)) },
        backgroundColor = MaterialTheme.colors.primary,
        actions = {
            ConnectButton(connected = connected, onClick = onClick)
        })
}

@Composable
fun ConnectButton(
    connected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, enabled = !connected) {
        if (connected)
            Icon(
                imageVector = vectorResource(
                    id = R.drawable.ic_twotone_check_24
                ), tint = MaterialTheme.colors.onPrimary
            )
        Text(
            text = stringResource(if (!connected) R.string.connect else R.string.connected),
            color = MaterialTheme.colors.onPrimary
        )
    }
}


@Preview
@Composable
fun AppTopBarPreview() {
    AppTopBar(connected = true) {}
}
