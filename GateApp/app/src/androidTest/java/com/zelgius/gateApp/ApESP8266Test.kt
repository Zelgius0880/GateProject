package com.zelgius.gateApp

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket

class ApES8266Test : TestCase() {

    @Test
    fun testConnection() {
        val socket = Socket("192.168.4.1", 1000)
        assertTrue(socket.isConnected)

        with(socket.getOutputStream()) {
            write("Hello\n".toByteArray())
            flush()
            Thread.sleep(100)
            close()
        }
    }
}