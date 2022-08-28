package com.zelgius.gateApp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import javax.inject.Inject


@AndroidEntryPoint
class GateOpeningService : Service() {
    @Inject
    lateinit var gateRepository: GateRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    companion object {
        var isRunning: Boolean =
            false // it's not very pretty, but it's an easy way to check if it is running. As there is only one instance of the service at once, it should be ok

        const val DIRECTION_EXTRA = "DIRECTION_EXTRA"
        const val NOTIFICATION_ID = 1

        private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

        fun startForeground(context: Context, direction: Direction) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, GateOpeningService::class.java).apply {
                    putExtra(DIRECTION_EXTRA, direction.name)
                })
        }

        private fun createNotification(
            context: Context,
            side: GateSide,
            direction: Direction,
            progress: Int? = null
        ): Notification {
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel(context, "worker", context.getString(R.string.working))

            return NotificationCompat.Builder(context, "worker")
                .setContentTitle(context.getString(if (side == GateSide.Left) R.string.left_side else R.string.right_side))
                .setSubText( // FIxME see if setContent(RemoteView(getPackageName(), R.layout.remote_view)) is not needed
                    context.getString(if (direction == Direction.Open) R.string.opening_side else R.string.closing_side)
                )
                .setSmallIcon(R.drawable.ic_gate)
                .setContentIntent(pendingIntent)
                .apply {
                    setProgress(
                        100,
                        (progress ?: 0).coerceAtLeast(0).coerceAtMost(100),
                        progress == null
                    )
                }
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel(
            context: Context,
            channelId: String,
            channelName: String
        ): String {
            val chan =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            return channelId
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
            Direction.values().find { it.name == s }
        }
        if (direction == null) stopForeground(STOP_FOREGROUND_REMOVE)
        else {
            startForeground(NOTIFICATION_ID, createNotification(this, GateSide.Left, direction))

            serviceScope.launch {
                doWork(direction)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private suspend fun doWork(direction: Direction) {
        val targetStatus =
            if (direction == Direction.Close) GateStatus.CLOSED else GateStatus.OPENED

        val workingStatus =
            if (direction == Direction.Close) GateStatus.CLOSING else GateStatus.OPENING

        val favorite = getSharedPreferences("Default", Context.MODE_PRIVATE).getString(
            GateViewModel.FAVORITE_SIDE, null
        ).let {
            GateSide.values().find { side -> side.id == it } ?: GateSide.Left
        }

        val gates = if (direction == Direction.Open) favorite to !favorite
        else !favorite to favorite

        moveSide(gates.first, direction, workingStatus, targetStatus)
        moveSide(gates.second, direction, workingStatus, targetStatus)

    }

    private suspend fun moveSide(
        side: GateSide,
        direction: Direction,
        workingStatus: GateStatus,
        targetStatus: GateStatus
    ) = coroutineScope {
        if (gateRepository.getStatus(side) == targetStatus) return@coroutineScope

        gateRepository.setStatus(side, workingStatus)
        val (listenerProgress, flowProgress) = gateRepository.flowProgress(side)
        val progressJob = async {
            flowProgress.collectLatest {
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification(this@GateOpeningService, side, direction, it)
                )
            }
        }

        val (listenerStatus, flowStatus) = gateRepository.flowStatus(side)
        flowStatus.first {
            targetStatus == it
        }

        progressJob.cancelAndJoin()
        listenerStatus?.remove()
        listenerProgress?.remove()
    }
}

enum class Direction {
    Close, Open
}
