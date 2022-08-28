package com.zelgius.gateApp

import android.app.ActivityManager
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class GateTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    @Inject
    lateinit var gateRepository: GateRepository
    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val statusLeft = gateRepository.getStatus(GateSide.Left)
            val statusRight = gateRepository.getStatus(GateSide.Right)

            val direction = if(statusLeft == statusRight) {
                if(statusLeft == GateStatus.OPENED) Direction.Close else Direction.Open
            } else Direction.Open

            GateOpeningService.startForeground(this@GateTileService, direction)
        }
    }
}