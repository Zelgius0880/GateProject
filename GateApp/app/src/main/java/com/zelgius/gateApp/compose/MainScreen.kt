package com.zelgius.gateApp.compose

import android.content.Context
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.viewModel
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.R
import com.zelgius.gateApp.WifiViewModel
import com.zelgius.gateApp.compose.buttons.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed

@ExperimentalCoroutinesApi
val snackbarChannel = BroadcastChannel<SnackbarMessage>(Channel.CONFLATED)

@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalMaterialApi
@Composable
fun MainScreen(
    wifiViewModel: WifiViewModel,
    needRequestingPermission: ((Context, Boolean) -> Unit) -> Unit,
) {
    // decouple snackbar host state from scaffold state for demo purposes
// this state, channel and flow is for demo purposes to demonstrate business logic layer
    val snackbarHostState = remember { SnackbarHostState() }
// we allow only one snackbar to be in the queue here, hence conflated

    LaunchedEffect(snackbarChannel) {
        snackbarChannel.asFlow().collectIndexed { _, value ->
            val result = snackbarHostState.showSnackbar(
                message = value.text,
                actionLabel = value.action
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    value.actionClicked?.invoke()
                }
                SnackbarResult.Dismissed -> {
                    /* dismissed, no action needed */
                }
            }
        }
    }

    MaterialTheme {
        val state = rememberScaffoldState(snackbarHostState = snackbarHostState)
        Scaffold(
            scaffoldState = state,
            topBar = { AppTopBar(connected = wifiViewModel.connected == true) { wifiViewModel.connectToGateWifi() } },
            bodyContent = {
                ScrollableColumn {
                    CardStatus(
                        signalStrength = wifiViewModel.signal,
                        progress = wifiViewModel.progress,
                        action = wifiViewModel.status, Modifier.fillMaxWidth()
                    )

                    CardOpenClose(viewModel = wifiViewModel, Modifier.fillMaxWidth())
                    CardTime(viewModel = wifiViewModel, Modifier.fillMaxWidth())
                    CardConfiguration(wifiViewModel = wifiViewModel, Modifier.fillMaxWidth())
                }
            }
        )
    }
}

@Composable
fun ProgressIndicator(working: Boolean) {
    if (working) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            CircularProgressIndicator()
        }
    }
}

@ExperimentalCoroutinesApi
@Composable
fun ConnectionStateSnackbar(connectedState: Boolean?) {
    val connected = remember { connectedState }
    connected?.let {
        if (it) {
            snackbarChannel.offer(SnackbarMessage(stringResource(id = R.string.connected)))
        } else {
            snackbarChannel.offer(SnackbarMessage(stringResource(id = R.string.cannot_connect)))
        }
    }

}

class SnackbarMessage(
    val text: String,
    val action: String? = null,
    val actionClicked: (() -> Unit)? = null
)

@Preview
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        Scaffold(
            bodyContent = {
                ProgressIndicator(working = true)

                Column {
                    ConnectButton(connected = false) {}

                    Row {
                        StepOpenButton(wifiViewModel = null)
                        StepCloseButton(wifiViewModel = null)
                    }
                }
            }
        )
    }
}