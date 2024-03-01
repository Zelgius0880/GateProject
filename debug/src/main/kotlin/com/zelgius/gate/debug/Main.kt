package com.zelgius.gate.debug

import java.io.File

fun main() {

    val controller = GateControllerImpl()
    try {
        controller.run()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        controller.shutdown()
    }
}

