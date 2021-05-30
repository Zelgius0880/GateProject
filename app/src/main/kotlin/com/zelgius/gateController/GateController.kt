package com.zelgius.gateController

import kotlinx.coroutines.*

class GateController {

    private val repository = GateRepository()
    private var gate: GateWork? = null

    private var currentStateLeft = GateStatus.NOT_WORKING
    private var currentStateRight = GateStatus.NOT_WORKING

    private var initComplete = false

    private val controllerScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

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
        println("Start listening")
        initComplete = true
        repository.listenStatus { side, status ->
            println("Status has changed: $side , $status")

            if (side != null) {
                getSide(side, status)?.apply {
                    println("here")
                    if(job == null || job?.isCompleted == true || job?.isCancelled == true)stopWorks(null)
                    gate = this
                    start()
                }
            } else {
                if (status != GateStatus.NOT_WORKING) {
                    println("there")
                    stopWorks(null)

                    job = controllerScope.launch {

                        println("--- Starting Moving Right ---")
                        currentStateRight = status
                        GateOpening(networkService, GateSide.Right, repository).apply {
                            gate = this
                        }.run()

                        println("--- Starting Moving Left ---")
                        currentStateLeft = status
                        GateOpening(networkService, GateSide.Left, repository).apply {
                            gate = this
                        }.run()

                        repository.setStatus(GateStatus.NOT_WORKING)
                    }
                }
            }
        }
    }

    private fun getSide(
        side: GateSide,
        status: GateStatus,
    ): GateWork? =
        if ((side == GateSide.Left && currentStateLeft != status) ||
            (side == GateSide.Right && currentStateRight != status)
        ) {

            val oldStatus = if (side == GateSide.Left) {
                currentStateLeft
            } else {
                currentStateRight
            }

            if (side == GateSide.Left) {
                currentStateLeft = status
            } else {
                currentStateRight = status
            }

            when (status) {
                GateStatus.NOT_WORKING -> {
                    stopWorks(side)
                    null
                }
                GateStatus.OPENING -> {
                    if (oldStatus != GateStatus.OPENED) {
                        GateOpening(networkService, side, repository)
                    } else {
                        controllerScope.launch {
                            repository.setStatus(GateStatus.OPENED)
                        }
                        null
                    }
                }

                GateStatus.CLOSING -> {
                    if (oldStatus != GateStatus.CLOSED) {
                        GateOpening(networkService, side, repository)
                    } else {
                        controllerScope.launch {
                            repository.setStatus(GateStatus.CLOSED)
                        }
                        null
                    }
                }

                GateStatus.MANUAL_OPENING -> {
                    ManualGateOpening(networkService, side, true)
                }

                GateStatus.MANUAL_CLOSING -> {
                    ManualGateOpening(networkService, side, false)
                }

                else -> {
                    null
                }
            }
        } else null


    private fun stopWorks(side: GateSide?) {
        println("Stopping $side -> ${gate?.side}")
        if(job?.isActive == true) job?.cancel()
        if (side == null || side == gate?.side) {
            gate?.stop = true

            println("Stopping ${gate?.side}")
            runBlocking {
                gate?.await()
            }
            println("Stopped ${gate?.side}")
            gate = null
        }
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
            4 -> {
                runBlocking {
                    repository.setSignal(
                        mapNetwork(
                            packet.data.first().toLong()
                                .coerceAtLeast(MIN_VAL)
                                .coerceAtMost(MAX_VAL)
                        ).toInt()
                    )
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
        const val MAX_VAL = -50L // define maximum signal strength (in dBm)
        const val MIN_VAL = -100L // define minimum signal strength (in dBm)

    }
}