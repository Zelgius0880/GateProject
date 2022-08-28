package com.zelgius.gate.debug

import com.pi4j.io.serial.*
import com.pi4j.io.serial.Serial
import kotlinx.coroutines.*
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.concurrent.thread
import kotlin.random.Random

open class SerialIO(protected val serial: Serial) {
    companion object {
        fun createSerial(port: String): Serial {

            val config = SerialConfig()
            config
                .device(port)
                .baud(Baud._115200)
                .dataBits(DataBits._8)
                .parity(Parity.NONE)
                .stopBits(StopBits._1)
                .flowControl(FlowControl.NONE)
            return SerialFactory.createInstance().apply {
                open(config)
            }
        }
    }

    protected val inputStream: InputStream = serial.inputStream
    protected val outputStream: OutputStream = serial.outputStream

    private var out = PipedOutputStream()
    private var readStream = PipedInputStream(out)


    var stop = false

    open fun start() {
        Thread.sleep(2000) // waiting starting up
        startLogger()
        initialize()
        startCLI()
    }

    protected fun initialize() {
        while (readStream.available() > 0) {
            readStream.read()
        }
    }

    open fun startCLI() {
        thread {
            var line = ""

            while (line != "exit" && !stop) {
                if (line.isNotBlank()) {
                    outputStream.write("$line\r\n".toByteArray())
                    outputStream.flush()
                }

                print(">")
                line = kotlin.io.readLine() ?: ""
                println("\n$> $line")
            }

            stop = true
            //serial.disconnect()
        }
    }
    fun nextByteHex() = "%02X".format(Random.nextInt(256).toByte())

    open fun startLogger() {
        thread {
            println("=== UART Ready ===")

            while (!stop) {
                with(inputStream.read()) {

                    if (this >= 0) {
                        print(toChar())
                        out.write(this)
                    }
                }
            }
        }
    }


    protected suspend fun readLine() =
        withTimeout(3000L) {
            var s = ""

            val handler = CoroutineExceptionHandler { _, exception ->
                throw exception
            }
            GlobalScope.launch(handler) {
                val builder = StringBuilder()
                while (!builder.endsWith('\n')) {
                    val c = readStream.read()
                    if (c >= 0) {
                        print(c.toChar())
                        builder.append(c.toChar())
                    }
                }
                s = builder.toString()
            }.join()
            s
        }
}