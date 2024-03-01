package com.zelgius.gate.debug

import com.zelgius.gateController.Gate
import com.zelgius.gateController.GateSide

class GateImpl (
    side: GateSide
) : Gate(side){

    override fun stop() {
       println("Stopping $side")
    }

    override fun open() {
        println("Opening $side")

    }

    override fun close() {
        println("Closing $side")

    }
}