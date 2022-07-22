package com.zelgius.gate.debug

//const val serialPort = "/dev/cu.usbmodem0006828704901"

fun main() {
    val serial = SerialIO.createSerial("/dev/serial0")

    SerialIO(serial).start()
}

