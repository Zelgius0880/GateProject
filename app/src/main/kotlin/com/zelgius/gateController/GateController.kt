package com.zelgius.gateController

import com.pi4j.Pi4J
import com.pi4j.context.Context
import kotlinx.coroutines.*
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalState

class GateController() {

    private val repository = GateRepository()
    private var gate: GateWork? = null

    private var currentStateLeft = GateStatus.NOT_WORKING
    private var currentStateRight = GateStatus.NOT_WORKING

    private val controllerScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val contextRight = Pi4J.newAutoContext()
    private val contextLeft = contextRight
    private val gateRight = Gate(contextRight, 17, 12, GateSide.Right)
    private val gateLeft = Gate(contextLeft, 19, 26, GateSide.Left)

    val light = contextRight.create(
        DigitalOutput.newConfigBuilder(contextRight)
            .id("light")
            .name("Light")
            .address(23)
            .shutdown(DigitalState.LOW)
            .initial(DigitalState.LOW)
            .provider("pigpio-digital-output")
    )


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
            if(it) light.high()
            else light.low()
        }
    }

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

    fun shutdown() {
        println("... Shutting down ...")
        contextRight.shutdown()
        contextLeft.shutdown()
    }
}