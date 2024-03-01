package com.zelgius.gateController

import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalState
import java.io.FileInputStream

class GateControllerImpl : GateController(FileInputStream("piclock-c9af5-firebase-adminsdk-bt8eu-7577a0e07b.json")) {

    private val contextRight = Pi4J.newAutoContext()
    private val contextLeft = contextRight

    override val gateRight = GateImpl(contextRight, 17, 12, GateSide.Right)
    override val gateLeft = GateImpl(contextLeft, 19, 26, GateSide.Left)

    private val light = contextRight.create(
        DigitalOutput.newConfigBuilder(contextRight)
            .id("light")
            .name("Light")
            .address(23)
            .shutdown(DigitalState.LOW)
            .initial(DigitalState.LOW)
            .provider("pigpio-digital-output")
    )


    override fun setLight(on: Boolean) {
        if(on) light.high()
        else light.low()
    }


    override fun shutdown() {
        println("... Shutting down ...")
        contextRight.shutdown()
        contextLeft.shutdown()
    }
}