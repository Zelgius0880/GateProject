package com.zelgius.gateApp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Socket

object TcpConnectionRepository {
    private var socket: Socket? = null
    private var listenerThread: ListenerThread? = null
    val messageChannel = BroadcastChannel<TcpMessage?>(Channel.CONFLATED)
    val isConnected: Boolean
        get() = socket?.isConnected ?: false

    suspend fun start() = withContext(Dispatchers.IO) {
        if(socket?.isConnected != true) {
            socket = Socket("192.168.4.1", 1000).also {
                listenerThread = ListenerThread(socket = it) { s ->
                    if (s == null) {
                        messageChannel.offer(null)
                        stop()
                    } else {
                        messageChannel.offer(s.toMessage())
                    }
                }.apply {
                    start()
                }
            }
        }
    }

    suspend fun send(msg: String) = withContext(Dispatchers.IO) {
        socket?.getOutputStream()?.writer()?.apply {
            if (!msg.endsWith("\r\n"))
                write("$msg\r\n")
            else
                write(msg)

            flush()
        }
    }

    fun stop() {
        listenerThread?.stop = true

        try {
            socket?.close()
            socket = null
        } catch (e: IOException) {
        }
    }


    class ListenerThread(private val socket: Socket, private val callback: (String?) -> Unit) :
        Thread() {
        var stop = false
        private val reader = socket.getInputStream().bufferedReader()
        override fun run() {
            while (!stop) {
                try {
                    val s = reader.readLine()
                    callback(s)
                } catch (e: IOException) {
                    e.printStackTrace()
                    stop = true

                    try {
                        socket.close()
                    } catch (e: IOException) {
                    }

                    callback(null)
                }
            }
        }
    }

    fun String.toMessage(): TcpMessage {
        val data = substringAfter("[").substringBefore("]").split(";")
        println(data.joinToString())
        return TcpMessage(protocol = data.first().toInt(), data = data.subList(0, data.size))
    }
}

data class TcpMessage(val protocol: Int, val data: List<String>)

