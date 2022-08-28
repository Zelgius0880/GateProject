package com.zelgius.gateApp

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GateRepository : FirebaseRepository() {

    suspend fun setTime(side: GateSide, time: Long) =
        set("gate_${side.id}", "states", mapOf("time" to time))


    suspend fun getStatus(side: GateSide): GateStatus =
        GateStatus.valueOf(getSnapshot("gate_${side.id}", "states").let {
            it["status"] as String
        })


    suspend fun setStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("status" to status))
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

