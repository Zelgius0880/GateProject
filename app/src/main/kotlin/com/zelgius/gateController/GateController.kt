package com.zelgius.gateController

import com.pi4j.io.serial.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class GateController {

    var gateId: Int = -1
    var appId: Int = -1

    val channel = Channel<String>(Channel.UNLIMITED)
    lateinit var reader: SerialReader

    val repository = GateRepository()
    var work: GateWork? = null
    var config: GateConfig? = null

    var currentState = GateStatus.NOT_WORKING

    var initComplete = false

    fun run() {
        val config = SerialConfig()
        config
            .device(serialPort)
            .baud(Baud._115200)
            .dataBits(DataBits._8)
            .parity(Parity.NONE)
            .stopBits(StopBits._1)
            .flowControl(FlowControl.NONE)
        val serial = SerialFactory.createInstance()
        serial.open(config)
        println("=== UART Ready $serialPort ===")

        reader = SerialReader(channel, serial)
        reader.start()
        serial.write("AT+RST\r\n")

        runBlocking {
            repository.setStatus(repository.getCurrentStatus())
            repository.setSignal(-1)

            while (true) {
                val line = channel.receive()

                when {
                    line.startsWith("ready") -> {
                        serial.write("AT+RST\r\n")
                        setupESP8266(serial, channel)
                    }
                    line.startsWith("+IPD") -> delay(handleData(line, serial))
                    line.contains(",CLOSED") -> {
                        line.split(",").firstOrNull()?.toIntOrNull()?.let {
                            if (it == appId) appId = -1
                            else if (it == gateId) {
                                repository.setSignal(-1)
                                gateId = -1
                            }
                        }
                    }
                }

            }
        }
    }

    private fun startListening(serial: Serial) {
        initComplete = true
        repository.listenStatus {
            println("Status has changed: $it")
            if (currentState != it) {
                currentState = it
                when (it) {
                    GateStatus.NOT_WORKING -> {
                        stopWorks()
                    }
                    GateStatus.OPENING -> {
                        stopWorks()
                        work = GateWork(repository) { time ->
                            if (gateId >= 0) {
                                send(gateId, "[2;0;$time]\r\n", serial)
                                true
                            } else {
                                stopWorks()
                                false
                            }
                        }.apply { start() }
                    }

                    GateStatus.CLOSING -> {
                        stopWorks()
                        work = GateWork(repository) { time ->
                            if (gateId >= 0) {
                                send(gateId, "[2;1;$time]\r\n", serial)
                                true
                            } else {
                                stopWorks()
                                false
                            }
                        }.apply { start() }
                    }

                    else -> {
                    }
                }
            }
        }
    }

    private fun stopWorks() {
        work?.stop = true
        work?.join()
        config?.stop = true
        config?.join()
        config = null
        work = null
    }

    fun handleData(line: String, serial: Serial): Long {
        val packet = line.toPacket()

        return when (packet.protocol) {
            0 -> {
                if (packet.data.first() == "0") {
                    gateId = packet.linkId
                    if (!initComplete)
                        startListening(serial)
                } else appId = packet.linkId

                //send(packet.linkId, "[2;0;1000]\r\n", serial)
                0
            }

            2 -> {
                send(gateId, "[2;${packet.data[0]};${packet.data[1]}]\r\n", serial)
                stopWorks()
                packet.data[1].toLong()
            }

            3 -> {
                stopWorks()
                if (packet.data.first() == "0") {
                    config = GateConfig(repository) { time ->
                        if (gateId >= 0) {
                            send(gateId, "[2;0;$time]\r\n", serial)
                            true
                        } else false
                    }.apply {
                        start()
                    }
                } else {
                    config?.stop = true
                }
                0
            }


            4 -> {
                runBlocking {
                    repository.setSignal(map(packet.data.first().toLong(), MIN_VAL, MAX_VAL, 0L, 5L).toInt())
                }
                0
            }
            else -> 0
        }

    }

    fun setupESP8266(serial: Serial, channel: Channel<String>) {
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

    fun send(linkId: Int, s: String, serial: Serial) {
        reader.readyToSend = {
            it.write(s)
            reader.readyToSend = null
        }
        serial.write("AT+CIPSEND=$linkId,${s.length}\r\n")

    }

    data class Packet(val protocol: Int, val linkId: Int, val data: List<String>)

    fun String.toPacket(): Packet {
        // +IPD,0,7:[0,0]
        val info = split(",")
        val data = info[2].substringAfter("[").substringBefore("]").split(";")
        println(data.joinToString())
        return Packet(
            linkId = info[1].toInt(),
            protocol = data.first().toInt(),
            data = data.subList(1, data.size)
        ).apply { println(this) }
    }

    fun map(x: Long, in_min: Long, in_max: Long, out_min: Long, out_max: Long): Long {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }


    companion object {
        //const val serialPort = "/dev/ttyS0"
        const val serialPort = "/dev/serial0"
        const val MAX_VAL = -20L // define maximum signal strength (in dBm)
        const val MIN_VAL = -80L // define minimum signal strength (in dBm)

    }
}