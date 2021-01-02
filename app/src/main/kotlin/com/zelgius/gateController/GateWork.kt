package com.zelgius.gateController

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

class GateWork(private val repository: GateRepository, val send: (Long) -> Boolean) : Thread(){

    var stop = false
    override fun run() {
        runBlocking {
            val movingTime = repository.getTime()
            val current = repository.getCurrentStatus()
            var progress = repository.getProgress()
            val status = repository.getStatus()

            val isOpen: Boolean
             if(progress < 100) {
                progress = when {
                    current == GateStatus.OPENING && status == GateStatus.CLOSING -> {
                        isOpen = false
                        100 - progress
                    }
                    current == GateStatus.OPENING && status == GateStatus.OPENING -> {
                        isOpen = true
                        progress
                    }
                    current == GateStatus.CLOSING && status == GateStatus.OPENING -> {
                        isOpen = true
                        100 - progress
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
                    if(it) {
                        repository.setCurrentStatus(GateStatus.CLOSING)
                    } else {
                        repository.setCurrentStatus(GateStatus.OPENING)
                    }
                }
            }

            var time = (movingTime*(progress/100f)).roundToInt()

            repository.setProgress(progress)
            repository.setCurrentStatus(if(isOpen) GateStatus.OPENING else GateStatus.CLOSING)

            val startingTime = System.currentTimeMillis()
            while (time < movingTime && !stop && progress < 100) {
                val milli1 = System.currentTimeMillis()
                stop = stop || !send(500)
                time += 500
                progress = (100f * time / movingTime).roundToInt()
                repository.setProgress(progress)

                val milli2 = System.currentTimeMillis()

                if(milli2 - milli1 < 500)
                    delay(500 - (milli2 - milli1))
            }

            println("Moving during ${ System.currentTimeMillis() - startingTime}/$movingTime ms")
            if(time > movingTime) {
                send(time - movingTime)
            }

            if(time >= movingTime ||progress >= 100) {
                repository.setProgress(100)
                repository.setStatus(if (isOpen) GateStatus.OPENED else GateStatus.CLOSED)
                repository.setCurrentStatus(if (isOpen) GateStatus.OPENED else GateStatus.CLOSED)
            }
        }

    }

}