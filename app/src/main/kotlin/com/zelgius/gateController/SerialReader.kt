package com.zelgius.gateController

import com.pi4j.io.serial.Serial
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

class SerialReader(val channel: Channel<String>, private val serial: Serial) : Thread() {
    var stop = false
        set(value) {
            field = value

            if (value) input.close()
        }
    private val input = serial.inputStream
    var readyToSend: ((Serial) -> Unit)? = null

    override fun run() {
        val buffer = StringBuffer()
        while (!stop) {
            val b = input.read()
            print(b.toChar())
            if (b != -1) {
                buffer.append(b.toChar())

                when (b.toChar()) {
                    '\n' -> {
                        channel.offer(buffer.toString())
                        buffer.delete(0, buffer.length)
                        print('>')
                    }
                    '>' -> readyToSend?.invoke(serial)
                }

                if (input.toString() == "ready")
                    channel.offer(buffer.toString())
            }
        }
    }
}