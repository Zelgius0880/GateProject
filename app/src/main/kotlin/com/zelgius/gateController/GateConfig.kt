package com.zelgius.gateController

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class GateConfig(
    private val side: GateSide,
    private val repository: GateRepository,
    val send: (Long) -> Boolean
) : Thread() {

    var stop = false
    override fun run() {
        runBlocking {

            var time = 0L
            while (!stop) {
                send(500)
                time += 500
                delay(500)
            }


            repository.setProgress(side, 0)
            repository.setCurrentStatus(side, GateStatus.OPENED)
            repository.setStatus(side, GateStatus.OPENED)
            repository.setTime(side, time)
        }
    }

}