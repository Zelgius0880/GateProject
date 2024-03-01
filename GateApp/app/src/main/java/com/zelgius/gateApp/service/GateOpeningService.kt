package com.zelgius.gateApp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ca.rmen.sunrisesunset.SunriseSunset
import com.zelgius.gateApp.BuildConfig
import com.zelgius.gateApp.GateRepository
import com.zelgius.gateApp.GateSide
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.GateViewModel
import com.zelgius.gateApp.MainActivity
import com.zelgius.gateApp.R
import com.zelgius.gateApp.service.Work.Companion.NOTIFICATION_ID
import com.zelgius.gateApp.service.Work.Companion.createNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@AndroidEntryPoint
class GateOpeningService : Service() {

    @Inject
    lateinit var work: Work

    companion object {
        var isRunning: Boolean =
            false // it's not very pretty, but it's an easy way to check if it is running. As there is only one instance of the service at once, it should be ok

        const val DIRECTION_EXTRA = "DIRECTION_EXTRA"

        private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

        fun startForeground(context: Context, direction: Direction) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, GateOpeningService::class.java).apply {
                    putExtra(DIRECTION_EXTRA, direction.name)
                })
        }

    }
    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val direction = intent?.getStringExtra(DIRECTION_EXTRA)?.let { s ->
            Direction.entries.find { it.name == s }
        }
        if (direction == null) stopForeground(STOP_FOREGROUND_REMOVE)
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    createNotification(this, GateSide.Left, direction),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification(this, GateSide.Left, direction))
            }

            serviceScope.launch {
                work.doWork(direction)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}