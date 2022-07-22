package com.zelgius.gate.scanner

import com.pi4j.io.serial.*

//const val serialPort = "/dev/cu.usbmodem0006828704901"

fun main() {
    Baud.values().forEach { baud ->
        println("=== $baud ====")

        val config = SerialConfig()
        config
            .device("/dev/serial0")
            .baud(baud)
            .dataBits(DataBits._8)
            .parity(Parity.NONE)
            .stopBits(StopBits._1)
            .flowControl(FlowControl.NONE)

        SerialFactory.createInstance().apply {
            open(config)

            write("AT+RST\r\n")

            inputStream.use {
                for(i in 0 until 10) {
                    Thread.sleep(100)
                    while(available() > 0) {
                        val b = it.read()
                        if(b != -1) print(b.toChar())
                    }
                }
            }

            Thread.sleep(3000)

            close()
        }

    }
}


