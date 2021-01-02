package com.zelgius.gateController

class GateRepository : FirebaseRepository() {
    suspend fun getProgress(): Int =
        getSnapshot("gate", "states").let {
            (it["progress"] as Long).toInt()
        }


    suspend fun setProgress(progress: Int) =
        set("gate" , "states", mapOf("progress" to progress))

    suspend fun getTime(): Long =
        getSnapshot("gate", "states").let {
            (it["time"] as Long)
        }


    suspend fun setTime(time: Long) =
        set("gate" , "states", mapOf("time" to time))


    suspend fun getStatus(): GateStatus =
        GateStatus.valueOf(getSnapshot("gate", "states").let {
            it["status"] as String
        })


    suspend fun setStatus(status: GateStatus) {
        set("gate" , "states", mapOf("status" to status))
    }


    suspend fun getCurrentStatus(): GateStatus =
        GateStatus.valueOf(getSnapshot("gate", "states").let {
            it["current"] as String
        })


    suspend fun setCurrentStatus(status: GateStatus) {
        set("gate" , "states", mapOf("current" to status))
    }



    suspend fun setSignal(signal: Int) {
        set("gate" , "states", mapOf("signal" to signal))
    }


    fun listenStatus(callback: (GateStatus) -> Unit) {
        listen("gate", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateStatus.valueOf(it["status"].toString()))
            }
        }
    }
}

enum class GateStatus {
    OPENING, CLOSING, NOT_WORKING, OPENED, CLOSED
}