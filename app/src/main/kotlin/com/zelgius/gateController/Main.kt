package com.zelgius.gateController

import com.pi4j.Pi4J
import java.io.File


fun main() {
    println(File("").absolutePath)

    val controller = GateController()
    try {
        controller.run()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        controller.shutdown()
    }

}