package com.zelgius.gateController

interface NetworkService {
    val onConnectionClosed: suspend () -> Unit

    var appId: Int
    var gateId: Int

    // The return is the time to delay
    suspend fun start(onDataReceived: (Packet) -> Long)
    fun sendToApp(data: String): Boolean
    fun sendToGate(data: String): Boolean


}



fun String.toPacket(): Packet {
    // +IPD,0,7:[0,0]
    val info = split(",")
    val data = info[2].substringAfter("[").substringBefore("]").split(";")
    println(data.joinToString())
    return Packet(
        linkId = info[1].toInt(),
        protocol = data.first().toInt(),
        data = data.subList(1, data.size)
    ).apply { println(this) }
}

data class Packet(val protocol: Int, val linkId: Int, val data: List<String>)
