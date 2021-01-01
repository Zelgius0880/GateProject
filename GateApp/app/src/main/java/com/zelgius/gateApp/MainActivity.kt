package com.zelgius.gateApp

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import com.zelgius.gateApp.compose.MainScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview


class MainActivity : AppCompatActivity() {

    lateinit var wifiViewModel: WifiViewModel

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map: Map<String, Boolean> ->
        permissionCallback?.invoke(this, map.values.all { it })
        permissionCallback = null
    }

    var permissionCallback: ((Context, Boolean) -> Unit)? = null

    @FlowPreview
    @ExperimentalCoroutinesApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiViewModel = ViewModelProvider(this)[WifiViewModel::class.java]
        setContent {
            val connectedState = mutableStateOf<Boolean?>(null)
            val workingState = mutableStateOf<Boolean>(false)

            MainScreen(
                wifiViewModel = wifiViewModel,
                needRequestingPermission = {
                    permissionCallback = it
                    permissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
            )
        }
    }
}
