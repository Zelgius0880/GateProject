package com.zelgius.gateApp.compose.buttons

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zelgius.gateApp.R

@Composable
fun AppTopBar() {
    TopAppBar(
        title = { Text(stringResource(id = R.string.gate_project)) },
        backgroundColor = MaterialTheme.colors.primary)
}

@Preview
@Composable
fun AppTopBarPreview() {
    AppTopBar()
}

