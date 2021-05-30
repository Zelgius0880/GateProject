package com.zelgius.gateController

import com.pi4j.io.gpio.*
import com.pi4j.io.serial.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ESP8266NetworkService(
    override val onConnectionClosed: suspend () -> Unit
) : NetworkService {
    private val serial: Serial
    private val reader: SerialReader
    private val channel = Channel<String>(Channel.UNLIMITED)

    private val red: GpioPinDigitalOutput
    private val blue: GpioPinDigitalOutput
    private val green: GpioPinDigitalOutput

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

        // create gpio controller
        GpioFactory.getInstance().apply {
            red = provisionDigitalOutputPin(RaspiPin.GPIO_09, PinState.LOW)
            blue = provisionDigitalOutputPin(RaspiPin.GPIO_08, PinState.LOW)
            green = provisionDigitalOutputPin(RaspiPin.GPIO_07, PinState.LOW)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun start(onDataReceived: (Packet) -> Long) {
        while (true) {
            val line = channel.receive()


            try {
                when {
                    line.startsWith("ready") -> {
                        red()
                        serial.write("AT+RST\r\n")

                        withTimeout(5000L) {
                            setupESP8266()
                        }
                    }
                    line.startsWith("+IPD") -> {
                        green()
                        delay(onDataReceived(line.toPacket()))
                    }
                    line.contains(",CLOSED") -> {
                        line.split(",").firstOrNull()?.toIntOrNull()?.let {
                            if (it == appId) appId = -1
                            else if (it == gateId) {
                                blue()
                                gateId = -1
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                serial.write("AT+RST\r\n")
            }
        }
    }


    override suspend fun sendToGate(data: String): Boolean {
        if (gateId >= 0) send(gateId, data)

        return gateId >= 0
    }

    private suspend fun send(linkId: Int, data: String) =
        suspendCoroutine<Unit> { continuation ->
            reader.readyToSend = {
                it.write(data)
                reader.readyToSend = null
                continuation.resume(Unit)
            }
            println("AT+CIPSEND=$linkId,${data.length}\r\n")
            serial.write("AT+CIPSEND=$linkId,${data.length}\r\n")
        }


    private suspend fun setupESP8266() {
        println("Setting up")
        val cmds = listOf(
            "AT+CWMODE=2",
            "AT+CIPMUX=1",
            "AT+CWSAP_CUR=\"OcvIK7wKlYd7nA\",\"ICiATx7pebzFLg\",5,2,2,0",
            "AT+CIPSERVER=1,1000",
        )

        cmds.forEach {
            serial.write("$it\r\n")
            var line = ""
            while (
                !line.startsWith("ERROR")
                && !line.startsWith("OK")
            ) {
                    line = channel.receive()
            }
            delay(500)
        }
        println("Command sequence complete")
        blue()
    }

    private fun red() {
        red.high()
        blue.low()
        green.low()
    }

    private fun blue() {
        red.low()
        blue.high()
        green.low()
    }

    private fun green() {
        red.low()
        blue.low()
        green.high()
    }

}