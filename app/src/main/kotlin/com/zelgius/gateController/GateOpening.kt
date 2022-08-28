package com.zelgius.gateController

import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class GateOpening(
    gate: Gate,
    private val repository: GateRepository,
) : GateWork(gate) {
    override suspend fun run(): GateStatus {
        val state = initMovement(state())

        var (progress, movingTime) = state
        val initProgress = progress

        progress = initProgress
        var time = (movingTime * (progress / 100f)).roundToLong()

        repository.setProgress(side, progress)
        repository.setCurrentStatus(side, if (isOpen) GateStatus.OPENING else GateStatus.CLOSING)

        if(isOpen) gate.open() else gate.close()
        val startingTime = System.currentTimeMillis() - time
        var oldProgress = progress
        while (time < movingTime && !stop && progress < 100) {
            time = System.currentTimeMillis() - startingTime

            progress = (100f * time / movingTime).roundToInt()
            if(progress != oldProgress) {
                repository.setProgress(side, progress)
                oldProgress = progress
            }

            delay(1)
        }

        gate.stop()

        if (time >= movingTime || progress >= 100) {
            repository.setProgress(side, 100)
            repository.setStatus(side, if (isOpen) GateStatus.OPENED else GateStatus.CLOSED)
            repository.setCurrentStatus(side, if (isOpen) GateStatus.OPENED else GateStatus.CLOSED)
        }
        return if (isOpen) GateStatus.OPENED else GateStatus.CLOSED
    }


    private suspend fun initMovement(state: OpeningState): OpeningState {
        var (progress, _, current, status) = state

        if (progress < 100) {
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
                if (it) {
                    repository.setCurrentStatus(side, GateStatus.CLOSING)
                } else {
                    repository.setCurrentStatus(side, GateStatus.OPENING)
                }
            }
        }

        repository.setStatus(side, if (!isOpen) GateStatus.CLOSING else GateStatus.OPENING)

        return state.copy(progress = progress)
    }
    suspend fun state(): OpeningState {
        return OpeningState(
            movingTime = repository.getTime(side),
            current = repository.getCurrentStatus(side),
            progress = repository.getProgress(side),
            status = repository.getStatus(side),
        )
    }
}

data class OpeningState(
    val progress: Int = 0,
    val movingTime: Long = 0,
    val current: GateStatus = GateStatus.NOT_WORKING,
    val status: GateStatus = GateStatus.NOT_WORKING,
)
