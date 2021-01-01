package com.zelgius.gateController

import java.io.File

/*
const int MAX_VAL = -20; // define maximum signal strength (in dBm)
const int MIN_VAL = -80; // define minimum signal strength (in dBm)

int strength = map(rssi, MIN_VAL, MAX_VAL, 0, 11);

 */

fun main() {
    println(File("").absolutePath)
    GateController().run()
}