package com.zelgius.gateApp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.quicksettings.TileService
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
                val status = getStatus()

                val notifyManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel: NotificationChannel =
                    notifyManager.getNotificationChannel(getString(R.string.notification_channel))
                        ?: {
                            val name = getString(R.string.notification_channel)
                            val descriptionText = getString(R.string.channel_description)
                            val importance = NotificationManager.IMPORTANCE_MIN
                            val mChannel = NotificationChannel(
                                getString(R.string.notification_channel),
                                name,
                                importance
                            )
                            mChannel.description = descriptionText
                            // Register the channel with the system; you can't change the importance
                            // or other notification behaviors after this
                            val notificationManager =
                                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.createNotificationChannel(mChannel)
                            mChannel
                        }()

                val builder = NotificationCompat.Builder(this@GateTileService, channel.id)
                builder.setContentTitle(
                    "${
                        getString(
                            when (status) {
                                GateStatus.OPENING, GateStatus.OPENED -> R.string.closing
                                GateStatus.CLOSING, GateStatus.CLOSED -> R.string.opening
                                GateStatus.NOT_WORKING -> R.string.not_working
                            }
                        )
                    }..."
                )
                    .setProgress(100, 0, false)
                    .setSmallIcon(R.drawable.ic_gate)

                notifyManager.notify(1, builder.build())
                var registration: ListenerRegistration? = null

                registration = listenProgress {
                    builder.setProgress(100, it, false)
                    notifyManager.notify(1, builder.build())

                    if (it >= 100) {

                        serviceScope.launch {
                            val status = getStatus()
                            if (status == GateStatus.OPENED || status == GateStatus.CLOSED) {
                                registration?.remove()
                                notifyManager.cancel(1)
                            }
                        }

                    }
                }

                setStatus(
                    when (status) {
                        GateStatus.OPENING, GateStatus.OPENED -> GateStatus.CLOSING
                        GateStatus.CLOSING, GateStatus.CLOSED -> GateStatus.OPENING
                        GateStatus.NOT_WORKING -> GateStatus.NOT_WORKING
                    }
                )

            }
        }
    }
}