package com.zelgius.gateApp.compose

import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelgius.gateApp.*
import com.zelgius.gateApp.R
import com.zelgius.gateApp.compose.buttons.AppTopBar
import com.zelgius.gateApp.compose.buttons.CardOpenClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed

val LightColors
    @Composable get() = lightColors(
        primary = colorResource(id = R.color.primary),
        primaryVariant = colorResource(id = R.color.primaryVariant),
        onPrimary = colorResource(id = R.color.white),
        secondary = colorResource(id = R.color.teal_200),
        secondaryVariant = colorResource(id = R.color.teal_700),
        onSecondary = colorResource(id = R.color.black),
    )

val DarkColors
    @Composable get() = darkColors(
        primary = colorResource(id = R.color.primaryDark),
        primaryVariant = colorResource(id = R.color.primaryVariantDark),
        onPrimary = colorResource(id = R.color.black),
        secondary = colorResource(id = R.color.teal_200),
        secondaryVariant = colorResource(id = R.color.teal_700),
        onSecondary = colorResource(id = R.color.black),
    )

@Composable
fun MainScreen(
    viewModel: GateViewModel = hiltViewModel(),
) {

    val scaffoldState = rememberScaffoldState()

    Snackbar(viewModel.messageFlow, state = scaffoldState.snackbarHostState)

    MaterialTheme(colors = if (isSystemInDarkTheme()) DarkColors else LightColors) {

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = { AppTopBar() },
            content = {
                Column(
                    modifier = Modifier
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(it)
                ) {

                    val statusLeft by viewModel.leftStatus.observeAsState(GateStatus.NOT_WORKING)
                    val statusRight by viewModel.rightStatus.observeAsState(GateStatus.NOT_WORKING)

                    CardOpenClose(Modifier.fillMaxWidth(),
                        isOpened = when {
                            statusLeft == GateStatus.CLOSED && statusRight == GateStatus.CLOSED -> false
                            statusLeft == GateStatus.OPENED && statusRight == GateStatus.OPENED -> true
                            else -> null
                        },
                        onOpen = { viewModel.openGate() },
                        onClose = { viewModel.closeGate() },
                        onStop = { viewModel.stop() }
                    )

                    CardSide(GateSide.Left, viewModel = viewModel)
                    CardSide(GateSide.Right, viewModel = viewModel)
                }
            }
        )
    }
}

@Composable
fun Snackbar(flow: Flow<SnackbarMessage>, state: SnackbarHostState) {
    LaunchedEffect(flow) {
        flow.collectIndexed { _, message ->
            val result = state.showSnackbar(
                message = message.message,
                actionLabel = message.action?.message
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    message.action?.action?.work()
                }
                SnackbarResult.Dismissed -> {
                    /* dismissed, no action needed */
                }
            }
        }
    }
}

@Composable
@Preview
fun MainScreenPreview() {
    MainScreen(viewModel = GateViewModel(Application()))
}