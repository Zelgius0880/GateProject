package com.zelgius.gateController

abstract class Gate(
    val side: GateSide
) {

    abstract fun stop()

    abstract fun open()

    abstract fun close()
}