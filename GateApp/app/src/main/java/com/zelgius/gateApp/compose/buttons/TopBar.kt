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

