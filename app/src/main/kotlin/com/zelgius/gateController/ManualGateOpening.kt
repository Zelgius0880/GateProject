package com.zelgius.gateController

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class ManualGateOpening(
    networkService: NetworkService,
    side: GateSide,
    override var isOpen: Boolean
) : GateWork(networkService, side) {

    override suspend fun run() :GateStatus{
        while (!stop) {
            val milli1 = System.currentTimeMillis()
            stop = stop || !send()

            val milli2 = System.currentTimeMillis()

            if (milli2 - milli1 < PACKET_TIME)
                delay(PACKET_TIME - (milli2 - milli1))
        }

        return GateStatus.NOT_WORKING
    }
}

enum class GateSide(val id: String) {
    Left("left"), Right("right")
}