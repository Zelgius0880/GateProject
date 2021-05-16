package com.zelgius.gateApp

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WifiViewModel(val app: Application) : AndroidViewModel(app) {
    private val connectivityManager =
        app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    private val wifiManager = app.getSystemService(WIFI_SERVICE) as WifiManager

    private val repository = TcpConnectionRepository

    var connected by mutableStateOf<Boolean?>(null)
        private set

    var working by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0)
        private set

    var time by mutableStateOf(0L)
        private set

    var config by mutableStateOf(false)
        private set

    var status by mutableStateOf(GateStatus.NOT_WORKING)
        private set

    var signal by mutableStateOf(-1)
        private set

    private var _gateRepository: GateRepository? = null 
    private val gateRepository : GateRepository
    get() {
        if(_gateRepository == null) _gateRepository = GateRepository()
        return _gateRepository!!
    }

    init {
        viewModelScope.launch {
            gateRepository.listenProgress {
                progress = it
            }

            gateRepository.listenStatus {
                status = it
            }

            gateRepository.listenTime {
                time = it
            }

            gateRepository.listenSignal {
                signal = it
            }
        }
    }

    private val defaultNetworkCallback = object :
        ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            //take action when network connection is gained
            if (isWifiConnected("\"$ssid\"")) {
                viewModelScope.launch {
                    gateRepository.goOffline()
                    repository.start()
                    connected = repository.isConnected
                    repository.send("[0;1]")
                }
            } else disconnect()

        }

        override fun onLost(network: Network) {
            if (isWifiConnected("\"$ssid\"")) {
                disconnect()
            }

        }

        override fun onUnavailable() {
            if (connected == true) {
                disconnect()
            }
        }

    }

    fun disconnect() {
        connected = false
        repository.stop()

        viewModelScope.launch {
            gateRepository.goOnline()
        }
    }

    init {
        val connectivityManager =
            app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            connectivityManager?.bindProcessToNetwork(network)

            viewModelScope.launch {
                gateRepository.goOffline()
                repository.start()
                connected = repository.isConnected
            }
            working = false
            //connectivityManager?.registerDefaultNetworkCallback(defaultNetworkCallback)
        }

        override fun onLost(network: Network) {
            if (isWifiConnected("\"$ssid\"")) {
                disconnect()
                try {
                    connectivityManager?.unregisterNetworkCallback(this)
                    connectivityManager?.registerDefaultNetworkCallback(defaultNetworkCallback)
                } catch (e: Exception) {
                }
            }

        }

        override fun onUnavailable() {
            if (connected == true) {
                disconnect()
                try {
                    connectivityManager?.unregisterNetworkCallback(this)
                    connectivityManager?.registerDefaultNetworkCallback(defaultNetworkCallback)
                } catch (e: Exception) {
                }
            }
        }
    }

    private val ssid = BuildConfig.AP_SSID

    private val pwd = BuildConfig.AP_PASSWORD

    @Suppress("DEPRECATION")
    fun connectToGateWifi() {
        working = true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    try {
                        if (ActivityCompat.checkSelfPermission(
                                app,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val wifiConfig = WifiConfiguration()
                            wifiConfig.SSID = java.lang.String.format("\"%s\"", ssid)
                            wifiConfig.preSharedKey = String.format("\"%s\"", pwd)

                            val netId =
                                wifiManager.configuredNetworks.find { it.SSID == ssid }?.networkId
                                    ?: wifiManager.addNetwork(wifiConfig)
                            wifiManager.disconnect()
                            wifiManager.enableNetwork(netId, true)
                            wifiManager.reconnect()
                            //_connected.postValue(true)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        disconnect()
                    }
                    working = false
                }
            }
        } else {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pwd)
                .build()

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()

            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
            }


            try {
                connectivityManager?.unregisterNetworkCallback(defaultNetworkCallback)
            } catch (e: Exception) {
            }
            connectivityManager?.requestNetwork(networkRequest, networkCallback)
        }

    }

    private fun isWifiConnected(machineID: String): Boolean {
        if (wifiManager.isWifiEnabled) {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo?.ssid == machineID)
                return true
        }

        working = false
        return false
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    fun stepOpenGate(delay: Long = 5000L) {
        working = true
        viewModelScope.launch {
            repository.send("[2;0;$delay]")
            working = false
        }
    }

    fun stepCloseGate(delay: Long = 5000L) {
        working = true
        viewModelScope.launch {
            repository.send("[2;1;$delay]")
            working = false
        }
    }

    fun startConfig() {
        viewModelScope.launch {
            repository.send("[3;0]")
            config = true
        }
    }

    fun stopConfig() {
        viewModelScope.launch {
            repository.send("[3;1]")
            config = false
        }
    }


    fun openGate() {
        working = true
        viewModelScope.launch {
            gateRepository.setStatus(GateStatus.OPENING)
        }
    }

    fun closeGate() {
        working = true
        viewModelScope.launch {
            gateRepository.setStatus(GateStatus.CLOSING)
        }
    }

    fun setMovingTime(time: Long) {
        viewModelScope.launch {
            gateRepository.setTime(time)
        }
    }


    fun forceClose() {
        viewModelScope.launch {
            gateRepository.setStatus(GateStatus.CLOSED)
            gateRepository.setCurrentStatus(GateStatus.CLOSED)
        }
    }

    fun stop() {
        viewModelScope.launch {
            gateRepository.setStatus(GateStatus.NOT_WORKING)
        }
    }
}