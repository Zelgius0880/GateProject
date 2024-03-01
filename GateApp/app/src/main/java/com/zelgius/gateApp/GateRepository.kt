package com.zelgius.gateApp

import ca.rmen.sunrisesunset.SunriseSunset
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.last

class GateRepository : FirebaseRepository() {

    private var job: Job? = null
    suspend fun setTime(side: GateSide, time: Long) =
        set("gate_${side.id}", "states", mapOf("time" to time))


    suspend fun getStatus(side: GateSide): GateStatus =
        GateStatus.valueOf(getSnapshot("gate_${side.id}", "states").let {
            it["status"] as String
        })

    suspend fun getToken(): String =
        getSnapshot("registration", "states").let {
            it["token"] as String
        }


    suspend fun setStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("status" to status))
    }

    suspend fun setCurrentStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("current" to status))
    }

    suspend fun setProgress(side: GateSide, progress: Int) {
        set("gate_${side.id}", "states", mapOf("progress" to progress))
    }

    suspend fun setLightStatus(isOn: Boolean) {
        set("gate_light", "states", mapOf("is_on" to isOn))
    }

    suspend fun setLightTime(time: Long) {
        set("gate_light", "states", mapOf("time_after" to time))
    }

    suspend fun setFavorite(side: GateSide) {
        set("gate_light", "states", mapOf("favorite" to side))
    }

    suspend fun listenStatus(side: GateSide, callback: (GateStatus) -> Unit) = listOf(
        listen("gate_${side.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let { snapshot ->
                snapshot["status"]?.let {
                    callback(GateStatus.valueOf(it.toString()))
                }
            }
        },
    )


    suspend fun flowFavorite(): Pair<ListenerRegistration?, Flow<GateSide?>> {
        var listener: ListenerRegistration? = null
        val flow = callbackFlow {
            listener = listen("gate_light", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let { snapshot ->
                    snapshot["favorite"]?.let {
                        trySend(GateSide.valueOf(it.toString()))
                    }
                }
            }

            awaitClose { cancel() }
        }
        return listener to flow
    }

    suspend fun flowStatus(side: GateSide): Pair<ListenerRegistration?, Flow<GateStatus?>> {
        var listener: ListenerRegistration? = null
        val flow = callbackFlow {
            listener = listen("gate_${side.id}", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let { snapshot ->
                    snapshot["status"]?.let {
                        trySend(GateStatus.valueOf(it.toString()))
                    }
                }
            }

            awaitClose { cancel() }
        }
        return listener to flow
    }

    suspend fun listenProgress(side: GateSide, callback: (Int?) -> Unit) = listOf(
        listen("gate_${side.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let { snapshot ->
                snapshot.getLong("progress")?.let { progress ->
                    snapshot["status"]?.let {
                        val status = GateStatus.valueOf(it.toString())
                        callback(if (status.isWorking()) progress.toInt() else null)
                    }
                }
            }
        },
    )

    suspend fun flowProgress(side: GateSide): Pair<ListenerRegistration?, Flow<Int?>> {
        var listener: ListenerRegistration? = null
        val flow = callbackFlow {
            listener = listen("gate_${side.id}", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let { snapshot ->
                    snapshot.getLong("progress")?.let { progress ->
                        snapshot["status"]?.let {
                            val status = GateStatus.valueOf(it.toString())
                            trySend(if (status.isWorking()) progress.toInt() else null)
                        }
                    }
                }
            }
            awaitClose { cancel() }
        }

        return listener to flow
    }

    suspend fun flowLightStatus(): Pair<ListenerRegistration?, Flow<Boolean>> {
        var listener: ListenerRegistration? = null
        val flow = callbackFlow {
            listener = listen("gate_light", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let { snapshot ->
                    snapshot.getBoolean("is_on")?.let {
                        trySend(it)
                    }
                }
            }

            awaitClose { cancel() }
        }
        return listener to flow
    }

    suspend fun flowLightTime(): Pair<ListenerRegistration?, Flow<Long>> {
        var listener: ListenerRegistration? = null
        val flow = callbackFlow {
            listener = listen("gate_light", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let { snapshot ->
                    snapshot.getLong("time_after")?.let {
                        trySend(it)
                    }
                }
            }

            awaitClose { cancel() }
        }
        return listener to flow
    }

    suspend fun listenTime(callback: (GateSide, time: Long) -> Unit) {
        listen("gate_${GateSide.Left.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Left, (it["time"] as Long?) ?: 0L)
            }
        }

        listen("gate_${GateSide.Right.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Right, (it["time"] as Long?) ?: 0L)
            }
        }
    }

    suspend fun openLightForDuration(duration: Long) {
        if (!SunriseSunset.isDay(BuildConfig.LATITUDE, BuildConfig.LONGITUDE)) {
            job?.cancel()
            job = coroutineScope {
                async {
                    setLightStatus(true)
                    delay(duration)
                    setLightStatus(false)
                }
            }
        }
    }
}

enum class GateStatus {
    OPENING, CLOSING, NOT_WORKING, OPENED, CLOSED, MANUAL_OPENING, MANUAL_CLOSING;

    fun isWorking() =
        this == OPENING || this == CLOSING || this == MANUAL_CLOSING || this == MANUAL_OPENING
}

enum class GateSide(val id: String) {
    Left("left"), Right("right");

    operator fun not() = when (this) {
        Left -> Right
        Right -> Left
    }
}

