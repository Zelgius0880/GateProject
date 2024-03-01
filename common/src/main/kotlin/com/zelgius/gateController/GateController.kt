package com.zelgius.gateController

import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.InputStream

abstract class GateController(serviceAccount: InputStream) {

    private val repository = GateRepository(serviceAccount)
    private var gate: GateWork? = null

    private var currentStateLeft = GateStatus.NOT_WORKING
    private var currentStateRight = GateStatus.NOT_WORKING

    private val controllerScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    abstract val gateRight: Gate
    abstract val gateLeft: Gate
    fun run() {
        runBlocking {
            repository.setStatus(GateSide.Left, repository.getCurrentStatus(GateSide.Left))
            repository.setStatus(GateSide.Right, repository.getCurrentStatus(GateSide.Right))

            startListening()
            while (true);
        }
    }

    private fun startListening() {
        println("Start listening")

        repository.listenStatus(GateSide.Right) {
            getSide(gateRight, it)?.apply {
                if (job == null || job?.isCompleted == true || job?.isCancelled == true) stopWorks(GateSide.Right)
                gate = this
                start()
            }
        }

        repository.listenStatus(GateSide.Left) {
            getSide(gateLeft, it)?.apply {
                if (job == null || job?.isCompleted == true || job?.isCancelled == true) stopWorks(GateSide.Left)
                gate = this
                start()
            }
        }

        repository.listenLightStatus {
           setLight(it)
        }
    }

    abstract fun setLight(on: Boolean)

    private fun getSide(
        gate: Gate,
        status: GateStatus,
    ): GateWork? =
        if ((gate.side == GateSide.Left && currentStateLeft != status) ||
            (gate.side == GateSide.Right && currentStateRight != status)
        ) {

            val oldStatus : GateStatus
            if (gate.side == GateSide.Left) {
                oldStatus = currentStateLeft
                currentStateLeft = status
            } else {
                oldStatus = currentStateRight
                currentStateRight = status
            }

            when (status) {
                GateStatus.NOT_WORKING -> {
                    stopWorks(gate.side)
                    null
                }

                GateStatus.OPENING -> {
                    if (oldStatus != GateStatus.OPENED) {
                        GateOpening(gate, repository)
                    } else {
                        controllerScope.launch {
                            repository.setStatus(GateStatus.OPENED)
                        }
                        null
                    }
                }

                GateStatus.CLOSING -> {
                    if (oldStatus != GateStatus.CLOSED) {
                        GateOpening(gate, repository)
                    } else {
                        controllerScope.launch {
                            repository.setStatus(GateStatus.CLOSED)
                        }
                        null
                    }
                }

                GateStatus.MANUAL_OPENING -> {
                    ManualGateOpening(gate, true)
                }

                GateStatus.MANUAL_CLOSING -> {
                    ManualGateOpening(gate, false)
                }

                else -> null
            }
        } else null


    private fun stopWorks(side: GateSide?) {
        println("Stopping $side -> ${gate?.side}")
        if (job?.isActive == true) job?.cancel()
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

    abstract fun shutdown()
}