package com.zelgius.gateApp.compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelgius.gateApp.GateSide
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.GateViewModel
import com.zelgius.gateApp.R
import com.zelgius.gateApp.SnackbarMessage
import com.zelgius.gateApp.compose.buttons.AppTopBar
import com.zelgius.gateApp.compose.buttons.CardOpenClose
import com.zelgius.gateApp.compose.buttons.Light
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

    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            if (!it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSharedPreferences("Prefs", Context.MODE_PRIVATE).edit {
                    putBoolean("PERMISSION_ASKED", true)
                }

                viewModel.showSnackBar(
                    context.getString(R.string.no_foreground_notification),
                    context.getString(R.string.update) to {
                        val intent: Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, "worker")
                        context.startActivity(intent)
                    }
                )
            }
        }

    LaunchedEffect(null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.getSharedPreferences("Prefs", Context.MODE_PRIVATE).getBoolean("PERMISSION_ASKED", false)
            && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        )
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
                        .padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally
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

                    val lightIsOn by viewModel.lightIsOn.observeAsState(false)
                    val lightTime by viewModel.lightTime.observeAsState(initial = 0L)
                    Light(
                        isOn = lightIsOn,
                        time = lightTime,
                        modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
                        onTimeSet = { t -> viewModel.setLightTime(t) }) { isOn ->
                        viewModel.toggleLight(isOn)
                    }
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