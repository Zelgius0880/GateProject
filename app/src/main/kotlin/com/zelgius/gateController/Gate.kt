package com.zelgius.gateController

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

class Gate(
    private val side: GateSide,
    private val repository: GateRepository,
    val send: (Long) -> Boolean
) : Thread() {

    private var progress: Int = 0
    private var movingTime : Long = 0
    private var current : GateStatus = GateStatus.NOT_WORKING
    private var status : GateStatus = GateStatus.NOT_WORKING
    private var isOpen: Boolean = false

    var stop = false
    override fun run() {
        runBlocking {
            initMovement()

            val initProgress = progress

            progress = initProgress
            var time = (movingTime * (progress / 100f)).roundToInt()

            repository.setProgress(side, progress)
            repository.setCurrentStatus(side, if (isOpen) GateStatus.OPENING else GateStatus.CLOSING)

            val startingTime = System.currentTimeMillis()
            while (time < movingTime && !stop && progress < 100) {
                val milli1 = System.currentTimeMillis()
                stop = stop || !send(500)
                time += 500
                progress = (100f * time / movingTime).roundToInt()
                repository.setProgress(side,progress)

                val milli2 = System.currentTimeMillis()

                if (milli2 - milli1 < 500)
                    delay(500 - (milli2 - milli1))
            }

            println("Moving during ${System.currentTimeMillis() - startingTime}/$movingTime ms")
            if (time > movingTime) {
                send(time - movingTime)
            }
        }
    }


    private suspend fun initMovement() {
         movingTime = repository.getTime(side)
         current = repository.getCurrentStatus(side)
         progress = repository.getProgress(side)
         status = repository.getStatus(side)

        if (progress < 100) {
            progress = when {
                current == GateStatus.OPENING && status == GateStatus.CLOSING -> {
                    isOpen = false
                    //100 - progress
                    0
                }
                current == GateStatus.OPENING && status == GateStatus.OPENING -> {
                    isOpen = true
                    progress
                }
                current == GateStatus.CLOSING && status == GateStatus.OPENING -> {
                    isOpen = true
                    //100 - progress
                    0
                }
                current == GateStatus.CLOSING && status == GateStatus.CLOSING -> {
                    isOpen = false
                    progress
                }
                else -> {
                    isOpen = !(current == GateStatus.OPENED || current == GateStatus.OPENING)
                    progress
                }
            }
            (current == GateStatus.OPENED || current == GateStatus.OPENING)
        } else {
            progress = 0
            isOpen = !(current == GateStatus.OPENED || current == GateStatus.OPENING).also {
                if (it) {
                    repository.setCurrentStatus(side, GateStatus.CLOSING)
                } else {
                    repository.setCurrentStatus(side, GateStatus.OPENING)
                }
            }
        }
    }
}

enum class GateSide(val id: String) {
    Left("left"), Right("right")
}