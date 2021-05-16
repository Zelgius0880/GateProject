package com.zelgius.gateController

class GateRepository : FirebaseRepository() {
    suspend fun getProgress(side: GateSide): Int =
        getSnapshot("gate_${side.id}", "states").let {
            (it["progress"] as Long).toInt()
        }


    suspend fun setProgress(side: GateSide, progress: Int) =
        set("gate_${side.id}", "states", mapOf("progress" to progress))

    suspend fun getTime(side: GateSide): Long =
        getSnapshot("gate_${side.id}", "states").let {
            (it["time"] as Long)
        }


    suspend fun setTime(side: GateSide, time: Long) =
        set("gate_${side.id}" , "states", mapOf("time" to time))


    suspend fun getStatus(side: GateSide): GateStatus =
        GateStatus.valueOf(getSnapshot("gate_${side.id}", "states").let {
            it["status"] as String
        })


    suspend fun setStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("status" to status))
    }


    suspend fun getCurrentStatus(side: GateSide): GateStatus =
        GateStatus.valueOf(getSnapshot("gate_${side.id}", "states").let {
            it["current"] as String
        })


    suspend fun setCurrentStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("current" to status))
    }

    suspend fun setSignal(signal: Int) {
        set("states","gate", mapOf("signal" to signal))
    }

    fun listenStatus(callback: (GateSide, GateStatus) -> Unit) {
        listen("gate_${GateSide.Left.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Left, GateStatus.valueOf(it["status"].toString()))
            }
        }

        listen("gate_${GateSide.Right.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Right, GateStatus.valueOf(it["status"].toString()))
            }
        }
    }
}

enum class GateStatus {
    OPENING, CLOSING, NOT_WORKING, OPENED, CLOSED
}
