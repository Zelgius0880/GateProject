package com.zelgius.gateController

import kotlinx.coroutines.delay

class ManualGateOpening(
    gate: Gate,
    override var isOpen: Boolean
) : GateWork(gate) {

    override suspend fun run() :GateStatus{
        if(isOpen) gate.open() else gate.close()
        while (!stop) {
            delay(1)
        }

        gate.stop()
        println("ManualGateOpening Stopped ${gate.side}")

        return GateStatus.NOT_WORKING
    }
}

enum class GateSide(val id: String) {
    Left("left"), Right("right");
    operator fun not(): GateSide = if(this == Left) Right else Left
}