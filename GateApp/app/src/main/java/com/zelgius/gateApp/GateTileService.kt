package com.zelgius.gateApp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class GateTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            with(GateRepository()) {
                val favorite = getSharedPreferences("Default", Context.MODE_PRIVATE)
                    .getString(
                        GateViewModel.FAVORITE_SIDE, null
                    ).let {
                        GateSide.values().find { side -> side.id == it }
                    }

                val status = when (favorite) {
                    GateSide.Left -> getStatus(GateSide.Left)
                    GateSide.Right -> getStatus(GateSide.Right)
                    null -> getStatus(GateSide.Right)
                }

                val channelName = getString(R.string.notification_channel)
                val notifyManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                val channel: NotificationChannel =
                    notifyManager.getNotificationChannel(channelName).let {
                        if (it == null) notifyManager.createNotificationChannel(
                            NotificationChannel(
                                getString(R.string.notification_channel),
                                getString(R.string.notification_channel),
                                NotificationManager.IMPORTANCE_MIN
                            ).apply {
                                description = getString(R.string.channel_description)
                            }
                        )

                        notifyManager.getNotificationChannel(channelName)
                    }

                val builder = NotificationCompat.Builder(this@GateTileService, channel.id)
                builder.setContentTitle(
                    "${
                        getString(
                            when (status) {
                                GateStatus.OPENING, GateStatus.OPENED, GateStatus.MANUAL_OPENING -> R.string.closing
                                GateStatus.CLOSING, GateStatus.CLOSED, GateStatus.MANUAL_CLOSING -> R.string.opening
                                GateStatus.NOT_WORKING -> R.string.not_working
                            }
                        )
                    }..."
                )
                    .setProgress(100, 0, true)
                    .setSmallIcon(R.drawable.ic_gate)

                notifyManager.notify(1, builder.build())

                var registration: List<ListenerRegistration>? = null
                registration = listenStatus { side, s ->

                    Log.e("Gate status", "$side, $status, $s")
                    if(favorite == side) {
                        when (favorite) {
                            GateSide.Left -> serviceScope.launch {
                                if (status == GateStatus.OPENING && s == GateStatus.CLOSED ||
                                    status == GateStatus.OPENED && s == GateStatus.CLOSED ||
                                    status == GateStatus.CLOSED && s == GateStatus.OPENED ||
                                    status == GateStatus.CLOSING && s == GateStatus.OPENED) {
                                    registration?.forEach { it.remove() }
                                    notifyManager.cancel(1)
                                }
                            }
                            GateSide.Right -> serviceScope.launch {
                                if (status == GateStatus.OPENING && s == GateStatus.CLOSED ||
                                    status == GateStatus.OPENED && s == GateStatus.CLOSED ||
                                    status == GateStatus.CLOSED && s == GateStatus.OPENED ||
                                    status == GateStatus.CLOSING && s == GateStatus.OPENED) {
                                    registration?.forEach { it.remove() }
                                    notifyManager.cancel(1)
                                }
                            }
                            null -> serviceScope.launch {
                                if (s == GateStatus.NOT_WORKING) {
                                    notifyManager.cancel(1)
                                }
                            }
                        }
                    }

                }

                if(favorite != null) {
                    setStatus(
                        favorite,
                        when (status) {
                            GateStatus.OPENING, GateStatus.OPENED, GateStatus.MANUAL_OPENING -> GateStatus.CLOSING
                            GateStatus.CLOSING, GateStatus.CLOSED, GateStatus.MANUAL_CLOSING -> GateStatus.OPENING
                            GateStatus.NOT_WORKING -> GateStatus.NOT_WORKING
                        }
                    )
                } else {
                    setStatus(
                        when (status) {
                            GateStatus.OPENING, GateStatus.OPENED, GateStatus.MANUAL_OPENING -> GateStatus.CLOSING
                            GateStatus.CLOSING, GateStatus.CLOSED, GateStatus.MANUAL_CLOSING -> GateStatus.OPENING
                            GateStatus.NOT_WORKING -> GateStatus.NOT_WORKING
                        }
                    )
                }

            }
        }
    }
}