package com.zelgius.gateController

class GateRepository : FirebaseRepository() {
    suspend fun getProgress(side: GateSide): Int =
        getSnapshot("gate_${side.id}", "states").let {
            (it["progress"] as Long?)?.toInt()?: 0
        }


    suspend fun setProgress(side: GateSide, progress: Int) =
        set("gate_${side.id}", "states", mapOf("progress" to progress))

    suspend fun getTime(side: GateSide): Long =
        getSnapshot("gate_${side.id}", "states").let {
            (it["time"] as Long?)?: 0
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

    suspend fun setStatus(status: GateStatus) {
        set("gate", "states", mapOf("status" to status))
    }


    suspend fun getCurrentStatus(side: GateSide): GateStatus =
        (getSnapshot("gate_${side.id}", "states")["current"] as String?)?.let {
            GateStatus.valueOf(it)
        } ?: GateStatus.NOT_WORKING


    suspend fun setCurrentStatus(side: GateSide, status: GateStatus) {
        set("gate_${side.id}", "states", mapOf("current" to status))
    }

    suspend fun setSignal(signal: Int) {
        set("gate","states", mapOf("signal" to signal))
    }

    fun listenStatus(callback: (GateSide?, GateStatus) -> Unit) {

        db.collection("states")
            .whereEqualTo("gate", true)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) throw firebaseFirestoreException

                documentSnapshot?.documentChanges?.forEach { change ->
                    val side = (change.document["side"] as String?)?.let {  s ->
                        GateSide.values().find { it.id == s}
                    }

                    (change.document["status"] as String?)?.let {  s ->
                        GateStatus.values().find { it.name == s}
                    }?.let {
                        callback(side, it)
                    }
                }
            }
    }

    fun listenStatus(side: GateSide, callback: (GateStatus) -> Unit) {
        db.collection("states")
            .document("gate_${side.id}")
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) throw firebaseFirestoreException
                if(documentSnapshot != null) {
                    (documentSnapshot["status"] as String?)?.let { s ->
                        GateStatus.values().find { it.name == s }
                    }?.let {
                        callback(it)
                    }
                }
            }
    }

    fun listenLightStatus(callback: (Boolean) -> Unit) {
        db.collection("states")
            .document("gate_light")
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) throw firebaseFirestoreException
                if(documentSnapshot != null) {
                    (documentSnapshot["is_on"] as Boolean?)?.let {
                        callback(it)
                    }
                }
            }
    }
}

enum class GateStatus {
    OPENING, CLOSING, NOT_WORKING, OPENED, CLOSED, MANUAL_OPENING, MANUAL_CLOSING,
}
