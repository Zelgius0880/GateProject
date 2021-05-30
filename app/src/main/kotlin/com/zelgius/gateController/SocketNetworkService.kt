package com.zelgius.gateController

import java.io.IOException
import java.lang.Exception
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class SocketNetworkService(
    override val onConnectionClosed: suspend () -> Unit,
) : NetworkService {
    override var appId: Int = -1
    override var gateId: Int = -1

    private var socket: Socket? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun start(onDataReceived: (Packet) -> Long) {
        val serverSocket = ServerSocket(1000, 50, InetAddress.getByName("192.168.1.16"))

        thread {
            while (true) {
                serverSocket.accept().let {
                    socket?.close()
                    println("new connection")
                    socket = it
                }
            }
        }

        val buffer = StringBuffer()
        while (true) {
            try {
                socket?.let {
                    val b = it.inputStream.read()
                    if (b != -1) {
                        buffer.append(b.toChar())

                        when (b.toChar()) {
                            '\n' -> {
                                println("> $buffer")
                                onDataReceived("+IPD,0,7:$buffer".toPacket())
                                buffer.delete(0, buffer.length)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                socket?.close()
                socket = null
            }
        }


        //socket?.close()
        //serverSocket.close()
    }


    override suspend fun sendToGate(data: String): Boolean {
        return socket?.getOutputStream()?.let {
            try {
                it.write(data.toByteArray())
                it.flush()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }?: false
    }
}