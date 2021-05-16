package com.zelgius.gateController

import com.pi4j.io.serial.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class ESP8266NetworkService(
    override val onConnectionClosed: suspend () -> Unit
) : NetworkService {
    private val serial: Serial
    private val reader: SerialReader
    private val channel = Channel<String>(Channel.UNLIMITED)

    override var appId = -1
    override var gateId = -1

    companion object {
        //const val serialPort = "/dev/ttyS0"
        const val serialPort = "/dev/serial0"
    }

    init {
        val config = SerialConfig()
        config
            .device(serialPort)
            .baud(Baud._115200)
            .dataBits(DataBits._8)
            .parity(Parity.NONE)
            .stopBits(StopBits._1)
            .flowControl(FlowControl.NONE)
        serial = SerialFactory.createInstance()
        serial.open(config)
        println("=== UART Ready $serialPort ===")
        reader = SerialReader(channel, serial)
        reader.start()
        serial.write("AT+RST\r\n")

    }

    override suspend fun start(onDataReceived: (Packet) -> Long) {
        while (true) {
            val line = channel.receive()

            when {
                line.startsWith("ready") -> {
                    serial.write("AT+RST\r\n")
                    setupESP8266()
                }
                line.startsWith("+IPD") -> delay(onDataReceived(line.toPacket()))
                line.contains(",CLOSED") -> {
                    line.split(",").firstOrNull()?.toIntOrNull()?.let {
                        if (it == appId) appId = -1
                        else if (it == gateId) {
                            gateId = -1
                        }
                    }
                }
            }

        }
    }

    override fun sendToApp(data: String): Boolean {
        if(appId >= 0) send(appId, data)

        return appId >= 0
    }

    override fun sendToGate(data: String): Boolean {
        if(gateId >= 0) send(gateId, data)

        return gateId >= 0
    }

    private fun send(linkId: Int, data: String) {
        reader.readyToSend = {
            it.write(data)
            reader.readyToSend = null
        }
        serial.write("AT+CIPSEND=$linkId,${data.length}\r\n")
    }


    private fun setupESP8266() {
        println("Setting up")
        val cmds = listOf(
            "AT+CWMODE=2",
            "AT+CIPMUX=1",
            "AT+CWSAP_CUR=\"U0c5vEPY2xzC3i0WnweR\",\"KGaPoM7bfVzfW5bSyoAaQ\",5,2,2,0",
            "AT+CIPSERVER=1,1000",
        )

        cmds.forEach {
            serial.write("$it\r\n")
            var line = ""
            runBlocking {
                while (
                    !line.startsWith("ERROR")
                    && !line.startsWith("OK")
                ) {
                    line = channel.receive()
                }
                delay(500)
            }
        }
        println("Command sequence complete")
    }


}