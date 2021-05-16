package com.zelgius.gateController

import kotlinx.coroutines.runBlocking

class GateController {

    private val repository = GateRepository()
    private var gate: Gate? = null
    private var config: GateConfig? = null

    private var currentStateLeft = GateStatus.NOT_WORKING
    private var currentStateRight = GateStatus.NOT_WORKING

    private var initComplete = false

    private val networkService: NetworkService = ESP8266NetworkService(
        onConnectionClosed = {
            repository.setSignal(-1)
        }
    )

    fun run() {
        runBlocking {
            repository.setStatus(GateSide.Left, repository.getCurrentStatus(GateSide.Left))
            repository.setStatus(GateSide.Right, repository.getCurrentStatus(GateSide.Right))
            repository.setSignal(-1)

            networkService.start {
                handleData(it)
            }
        }
    }

    private fun startListening() {
        initComplete = true
        repository.listenStatus { side, status ->
            println("Status has changed: $status")
            if ((side == GateSide.Left && currentStateLeft != status) ||
                (side == GateSide.Right && currentStateRight != status)
            ) {

                val id = if (side == GateSide.Left) {
                    currentStateLeft = status
                    0
                } else {
                    currentStateRight = status
                    1
                }

                when (status) {
                    GateStatus.NOT_WORKING -> {
                        stopWorks()
                    }
                    GateStatus.OPENING -> {
                        stopWorks()
                        gate = Gate(side, repository) { time ->
                            if (networkService.sendToGate("[2;0;$time;$id]\r\n")) {
                                true
                            } else {
                                stopWorks()
                                false
                            }
                        }.apply { start() }
                    }

                    GateStatus.CLOSING -> {
                        stopWorks()
                        gate = Gate(side, repository) { time ->
                            if (networkService.sendToGate("[2;1;$time;$id]\r\n")) {
                                true
                            } else {
                                stopWorks()
                                false
                            }
                        }.apply { start() }
                    }

                    else -> {
                    }
                }
            }
        }
    }

    private fun stopWorks() {
        gate?.stop = true
        gate?.join()
        config?.stop = true
        config?.join()
        config = null
        gate = null
    }

    private fun handleData(packet: Packet): Long {

        return when (packet.protocol) {
            0 -> {
                if (packet.data.first() == "0") {
                    networkService.gateId = packet.linkId
                    if (!initComplete)
                        startListening()
                } else networkService.appId = packet.linkId
                0
            }

            2 -> {
                networkService.sendToGate("[2;${packet.data[0]};${packet.data[1]};0]\r\n")
                networkService.sendToGate("[2;${packet.data[0]};${packet.data[1]};1]\r\n")
                stopWorks()
                packet.data[1].toLong()
            }

            3 -> {
                stopWorks()
                if (packet.data.first() == "0") {
                    val side = if(packet.data[1] == "0") GateSide.Left else GateSide.Right
                    config = GateConfig(side, repository) { time ->
                        networkService.sendToGate("[2;0;$time;0]\r\n")
                    }.apply {
                        start()
                    }
                } else {
                    config?.stop = true
                }
                0
            }

            4 -> {
                runBlocking {
                    repository.setSignal(mapNetwork(packet.data.first().toLong()).toInt())
                }
                0
            }
            else -> 0
        }

    }


    private fun mapNetwork(x: Long): Long {
        return (x - MIN_VAL) * (5 - 0) / (MAX_VAL - MIN_VAL) + 0
    }

    companion object {
        const val MAX_VAL = -20L // define maximum signal strength (in dBm)
        const val MIN_VAL = -80L // define minimum signal strength (in dBm)

    }
}