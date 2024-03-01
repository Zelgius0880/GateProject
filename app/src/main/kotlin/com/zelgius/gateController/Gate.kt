package com.zelgius.gateController

import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalState

class GateImpl (
    private val context: Context,
    enableOpenPin: Int,
    enableClosePin: Int,
    side: GateSide
) : Gate (side){
    private val enableClose = buildPin("close", enableClosePin)
    private val enableOpen = buildPin("open", enableOpenPin)

    override fun stop() {
        enableOpen.low()
        enableClose.low()
    }

    override fun open() {
        enableOpen.high()
        enableClose.low()
    }

    override fun close() {
        enableOpen.low()
        enableClose.high()
    }

    private fun buildPin(id: String, pin: Int) = context.create(
        DigitalOutput.newConfigBuilder(context)
            .id("${id}_$side")
            .name("${id.uppercase()} $side")
            .address(pin)
            .shutdown(DigitalState.LOW)
            .initial(DigitalState.LOW)
            .provider("pigpio-digital-output")
    )
}