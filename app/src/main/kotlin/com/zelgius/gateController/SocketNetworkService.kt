package com.zelgius.gateController

class SocketNetworkService(

    override val onConnectionClosed: suspend () -> Unit,
) : NetworkService {
    override var appId: Int = -1
    override var gateId: Int = -1

    override suspend fun start(onDataReceived: (Packet) -> Long) {

    }

    override fun sendToApp(data: String): Boolean {
        return true
    }

    override fun sendToGate(data: String): Boolean {
        return true
    }
}