package com.zelgius.gateApp

class GateRepository : FirebaseRepository() {

    suspend fun setTime(side: GateSide, time: Long) =
        set("gate_${side.id}", "states", mapOf("time" to time))


    suspend fun getStatus(side: GateSide): GateStatus =
        GateStatus.valueOf(getSnapshot("gate_${side.id}", "states").let {
            it["status"] as String
        })


    suspend fun getStatus(): GateStatus =
        GateStatus.valueOf(getSnapshot("gate", "states").let {
            it["status"] as String
        })


    suspend fun setStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("status" to status))
    }

    suspend fun setStatus(status: GateStatus) {
        set("gate", "states", mapOf("status" to status))
    }

    suspend fun listenProgress(callback: (side: GateSide, Int) -> Unit) = listOf(
        GateSide.Left to listen(
            "gate${GateSide.Left}",
            "states"
        ) { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Left, (it["progress"] as Long?)?.toInt() ?: 0)
            }
        },

        GateSide.Right to listen(
            "gate${GateSide.Right}",
            "states"
        ) { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Right, (it["progress"] as Long?)?.toInt() ?: 0)
            }
        }
    )


    suspend fun listenSignal(callback: (Int) -> Unit) =
        listen("gate", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback((it["signal"] as Long?)?.toInt() ?: 0)
            }
        }

    suspend fun listenStatus(callback: (GateSide?, GateStatus) -> Unit)  = listOf(
            listen("gate_${GateSide.Left.id}", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let {
                    callback(GateSide.Left, GateStatus.valueOf(it["status"].toString()))
                }
            },
    
            listen("gate_${GateSide.Right.id}", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let {
                    callback(GateSide.Right, GateStatus.valueOf(it["status"].toString()))
                }
            },

            listen("gate", "states") { documentSnapshot, firestoreException ->
                if (firestoreException != null) throw firestoreException
                else documentSnapshot?.let {
                    callback(null, GateStatus.valueOf(it["status"].toString()))
                }
            }
        )

    suspend fun listenTime(callback: (GateSide, time: Long) -> Unit) {
        listen("gate_${GateSide.Left.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Left,(it["time"] as Long?) ?: 0L)
            }
        }

        listen("gate_${GateSide.Right.id}", "states") { documentSnapshot, firestoreException ->
            if (firestoreException != null) throw firestoreException
            else documentSnapshot?.let {
                callback(GateSide.Right,(it["time"] as Long?) ?: 0L)
            }
        }
    }
}

enum class GateStatus {
    OPENING, CLOSING, NOT_WORKING, OPENED, CLOSED, MANUAL_OPENING, MANUAL_CLOSING,
}

enum class GateSide(val id: String) {
    Left("left"), Right("right");

    operator fun not() = when (this) {
        Left -> Right
        Right -> Left
    }
}

