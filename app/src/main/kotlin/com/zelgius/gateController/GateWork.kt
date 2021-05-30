package com.zelgius.gateController

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class GateWork(
    private val networkService: NetworkService,
    val side: GateSide,
) {
    open var stop: Boolean = false
    open var isOpen: Boolean = false

    companion object {
        const val PACKET_TIME = 1000L
    }

    protected val scope = CoroutineScope(Dispatchers.IO)
    protected var job: Job? = null

    fun start() {
        job = scope.launch {
            run()
        }
    }

    abstract suspend fun run(): GateStatus

    open suspend fun await() {
        job?.cancel()
    }

    suspend fun send() = if (isOpen)
        sendOpeningCommand()
    else sendClosingCommand()

    private suspend fun sendOpeningCommand(): Boolean =
        networkService.sendToGate(
            "[2;0;$PACKET_TIME;${
                if (side == GateSide.Left) 1 else 0
            }]\r\n"
        )


    private suspend fun sendClosingCommand(): Boolean =
        networkService.sendToGate(
            "[2;1;$PACKET_TIME;${
                if (side == GateSide.Left) 1 else 0
            }]\r\n"
        )


}