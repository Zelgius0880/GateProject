package com.zelgius.gateApp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import ca.rmen.sunrisesunset.SunriseSunset
import com.zelgius.gateApp.BuildConfig
import com.zelgius.gateApp.GateRepository
import com.zelgius.gateApp.GateSide
import com.zelgius.gateApp.GateStatus
import com.zelgius.gateApp.GateViewModel
import com.zelgius.gateApp.MainActivity
import com.zelgius.gateApp.R
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Work @Inject constructor(
    private val context: Context,
    private val gateRepository: GateRepository,
    private val notificationManager: NotificationManager
) {
    suspend fun doWork(direction: Direction) {
        val targetStatus =
            if (direction == Direction.Close) GateStatus.CLOSED else GateStatus.OPENED

        val workingStatus =
            if (direction == Direction.Close) GateStatus.CLOSING else GateStatus.OPENING

        val (favoriteListener, favoriteFlow) = gateRepository.flowFavorite()
        val favorite = favoriteFlow.first()?: context.getSharedPreferences("Default", Context.MODE_PRIVATE).getString(
            GateViewModel.FAVORITE_SIDE, null
        ).let {
            GateSide.entries.find { side -> side.id == it } ?: GateSide.Left
        }
        favoriteListener?.remove()

        val gates = if (direction == Direction.Open) favorite to !favorite
        else !favorite to favorite

        val turnOnLight = !SunriseSunset.isDay(BuildConfig.LATITUDE, BuildConfig.LONGITUDE)
        if (turnOnLight) gateRepository.setLightStatus(true)

        moveSide(gates.first, direction, workingStatus, targetStatus)
        moveSide(gates.second, direction, workingStatus, targetStatus)

        if (turnOnLight) {
            val (timeListener, timeFlow) = gateRepository.flowLightTime()
            val (statusListener, statusFlow) = gateRepository.flowLightStatus()

            val time = timeFlow.first()

            val statingTime = Date().time

            while (Date().time - statingTime < time * 1000 && statusFlow.first()) {
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification(
                        context,
                        (time * 1000 - (Date().time - statingTime)).milliseconds
                    )
                )
                delay(1.seconds)
            }

            timeListener?.remove()
            statusListener?.remove()
            gateRepository.setLightStatus(false)
        }
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
                    createNotification(
                        context,
                        side,
                        direction,
                        it
                    )
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
        listenerProgress?.remove()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        fun createNotification(
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
                .setSubText(
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

        private fun createNotification(
            context: Context,
            time: Duration,
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

            val minutes = time.inWholeMinutes
            val seconds = (time - minutes.minutes).inWholeSeconds

            return NotificationCompat.Builder(context, "worker")
                .setContentTitle(context.getString(R.string.light_is_on))
                .setContentText(
                    context.getString(R.string.light_is_on_remaining_time, minutes, seconds)
                )
                .setSmallIcon(R.drawable.ic_gate)
                .setContentIntent(pendingIntent)
                .build()
        }


        fun createNotification(
            context: Context
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
                .setContentTitle(context.getString(R.string.working))
                .setContentText(
                    context.getString(R.string.working)
                )
                .setSmallIcon(R.drawable.ic_gate)
                .setContentIntent(pendingIntent)
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
}


enum class Direction {
    Close, Open
}
