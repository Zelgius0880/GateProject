package com.zelgius.gate.debug

import com.zelgius.gateController.GateController
import com.zelgius.gateController.GateSide
import java.io.File
import java.io.FileInputStream

class GateControllerImpl : GateController(File("debug/personal-iot-dev-firebase-adminsdk-d8mqo-bf31597815.json").inputStream()) {


    override val gateRight = GateImpl(GateSide.Right)
    override val gateLeft = GateImpl(GateSide.Left)



    override fun setLight(on: Boolean) {
        println("Setting light ${if(on) "On" else "Off"}")
    }


    override fun shutdown() {
        println("... Shutting down ...")
    }
}