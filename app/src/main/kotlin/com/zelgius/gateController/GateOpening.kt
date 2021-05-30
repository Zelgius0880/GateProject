package com.zelgius.gateController

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class GateOpening(
    networkService: NetworkService,
    side: GateSide,
    private val repository: GateRepository,
) : GateWork(networkService, side) {

    private var progress: Int = 0
    private var movingTime: Long = 0
    private var current: GateStatus = GateStatus.NOT_WORKING
    private var status: GateStatus = GateStatus.NOT_WORKING

    override suspend fun run(): GateStatus {
        initMovement()

        val initProgress = progress

        progress = initProgress
        var time = (movingTime * (progress / 100f)).roundToLong()

        repository.setProgress(side, progress)
        repository.setCurrentStatus(side, if (isOpen) GateStatus.OPENING else GateStatus.CLOSING)

        val startingTime = System.currentTimeMillis()
        while (time < movingTime && !stop && progress < 100) {
            val milli1 = System.currentTimeMillis()
            stop = stop || !send()
            time += PACKET_TIME
            progress = (100f * time / movingTime).roundToInt()
            repository.setProgress(side, progress)

            val milli2 = System.currentTimeMillis()

            if (milli2 - milli1 < PACKET_TIME)
                delay(PACKET_TIME - (milli2 - milli1))
        }

        println("Moving during ${System.currentTimeMillis() - startingTime}/$movingTime ms")
        if (time > movingTime) {
            send()
        }

        if (time >= movingTime || progress >= 100) {
            repository.setProgress(side, 100)
            repository.setStatus(side, if (isOpen) GateStatus.OPENED else GateStatus.CLOSED)
            repository.setCurrentStatus(side, if (isOpen) GateStatus.OPENED else GateStatus.CLOSED)
        }
        return if (isOpen) GateStatus.OPENED else GateStatus.CLOSED
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

        repository.setStatus(side, if (!isOpen) GateStatus.CLOSING else GateStatus.OPENING)
    }
}
