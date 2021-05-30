package com.zelgius.gateApp.compose

import android.app.Application
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.zelgius.gateApp.GateSide
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.GateViewModel
import com.zelgius.gateApp.SnackbarMessage
import com.zelgius.gateApp.compose.buttons.AppTopBar
import com.zelgius.gateApp.compose.buttons.CardOpenClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import androidx.compose.foundation.rememberScrollState

val LightColors = lightColors(
    background = Color(0xFFf5f5f5)
)

@Composable
fun MainScreen(
    viewModel: GateViewModel,
) {

    val scaffoldState = rememberScaffoldState()

    Snackbar(viewModel.messageFlow, state = scaffoldState.snackbarHostState)

    MaterialTheme(colors = LightColors) {
        val signal by viewModel.signal.observeAsState(0)

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = { AppTopBar(signal = signal) },
            content = {
                Column(modifier = Modifier.verticalScroll(
                    rememberScrollState()
                )) {

                    CardOpenClose(Modifier.fillMaxWidth(),
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