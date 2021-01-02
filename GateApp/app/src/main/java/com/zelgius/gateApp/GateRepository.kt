package com.zelgius.gateApp

import android.util.Log
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class GateRepository : FirebaseRepository() {
    suspend fun getProgress(): Int =
        getSnapshot("gate", "states").let {
            (it["progress"] as Long).toInt()
        }

    suspend fun goOffline() = suspendCoroutine<Unit> { cont ->
        db.disableNetwork().addOnCompleteListener {
            cont.resume(Unit)
        }
    }

    suspend fun goOnline() = suspendCoroutine<Unit> { cont ->
        db.enableNetwork().addOnCompleteListener {
            cont.resume(Unit)
        }
    }

    suspend fun setProgress(progress: Int) =
        set("gate", "states", mapOf("progress" to progress))

    suspend fun getTime(): Long =
        getSnapshot("gate", "states").let {
            (it["time"] as Long)
        }


    suspend fun setTime(time: Long) =
        set("gate", "states", mapOf("time" to time))


    suspend fun getStatus(): GateStatus =
        GateStatus.valueOf(getSnapshot("gate", "states").let {
            it["status"] as String
        })


    suspend fun setStatus(status: GateStatus) {
        set("gate", "states", mapOf("status" to status))
    }


    suspend fun getCurrentStatus(): GateStatus =
        GateStatus.valueOf(getSnapshot("gate", "states").let {
            it["current"] as String
        })


    suspend fun setCurrentStatus(status: GateStatus) {
        set("gate", "states", mapOf("current" to status))
    }

    suspend fun listenStatus(callback: (GateStatus) -> Unit) =
        listen("gate", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateStatus.valueOf(it["status"].toString()))
            }
        }


    suspend fun listenProgress(callback: (Int) -> Unit) =
        listen("gate", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback((it["progress"] as Long).toInt())
            }
        }


    suspend fun listenTime(callback: (Long) -> Unit) =
        listen("gate", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback((it["time"] as Long))
            }
        }


    suspend fun listenSignal(callback: (Int) -> Unit) =
        listen("gate", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback((it["signal"] as Long).toInt())
            }
        }

}

enum class GateStatus {
    OPENING, CLOSING, NOT_WORKING, OPENED, CLOSED;

    operator fun not() =
        when (this) {
            OPENING -> CLOSING
            CLOSING -> OPENING
            NOT_WORKING -> NOT_WORKING
            OPENED -> CLOSED
            CLOSED -> OPENED
        }

}
