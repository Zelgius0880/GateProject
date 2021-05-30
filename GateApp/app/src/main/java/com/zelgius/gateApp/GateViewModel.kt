package com.zelgius.gateApp

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.*


class GateViewModel(private val app: Application) : AndroidViewModel(app) {
    companion object {
        const val FAVORITE_SIDE = "FAVORITE_SIDE"
    }

    private val messageChannel = Channel<SnackbarMessage>()
    val messageFlow = messageChannel.receiveAsFlow()

    private val sharedPreferences = app.getSharedPreferences("Default", Context.MODE_PRIVATE)

    private val _timeRight = MutableLiveData(0L)
    val timeRight: LiveData<Long>
        get() = _timeRight

    private val _timeLeft = MutableLiveData(0L)
    val timeLeft: LiveData<Long>
        get() = _timeLeft

    private val _status = MutableLiveData(GateStatus.NOT_WORKING)
    val status: LiveData<GateStatus>
        get() = _status

    private val _leftStatus = MutableLiveData(GateStatus.NOT_WORKING)
    val leftStatus: LiveData<GateStatus>
        get() = _leftStatus

    private val _rightStatus = MutableLiveData(GateStatus.NOT_WORKING)
    val rightStatus: LiveData<GateStatus>
        get() = _rightStatus

    private val _signal = MutableLiveData(-1)
    val signal: LiveData<Int>
        get() = _signal

    private val _favorite = MutableLiveData<GateSide?>(null)
    val favorite: LiveData<GateSide?>
        get() = _favorite

    private var _gateRepository: GateRepository? = null
    private val gateRepository: GateRepository
        get() {
            if (_gateRepository == null) _gateRepository = GateRepository()
            return _gateRepository!!
        }

    private var manualOpeningLeft = 0L
    private var manualOpeningRight = 0L

    init {
        viewModelScope.launch {

            gateRepository.listenStatus { side, status ->
                _status.postValue(status)
                when (side) {
                    GateSide.Left -> this@GateViewModel._leftStatus.postValue(status)
                    GateSide.Right -> this@GateViewModel._rightStatus.postValue(status)
                }
            }

            gateRepository.listenTime { side, time ->
                when (side) {
                    GateSide.Left -> _timeLeft.postValue(time)
                    GateSide.Right -> _timeRight.postValue(time)
                }
            }

            gateRepository.listenSignal {
                _signal.postValue(it)
            }

            _favorite.value = sharedPreferences.getString(
                FAVORITE_SIDE, null
            ).let {
                GateSide.values().find { side -> side.id == it }
            }
        }
    }

    fun openGate() {
        viewModelScope.launch {
            gateRepository.setStatus(GateStatus.OPENING)
        }
    }

    fun closeGate() {
        viewModelScope.launch {
            gateRepository.setStatus(GateStatus.CLOSING)
        }
    }

    fun toggleFavorite(side: GateSide) {
        viewModelScope.launch {
            if (_favorite.value != side) {

                sharedPreferences.edit {
                    putString(FAVORITE_SIDE, side.id)
                }
                _favorite.value = side
            } else {
                _favorite.value = null

                sharedPreferences.edit {
                    putString(FAVORITE_SIDE, null)
                }
            }

        }
    }

    fun manualOpenGate(side: GateSide) {
        viewModelScope.launch {
            when (side) {
                GateSide.Left -> manualOpeningLeft = Date().time
                GateSide.Right -> manualOpeningRight = Date().time
            }
            gateRepository.setStatus(side, GateStatus.MANUAL_OPENING)

        }
    }

    fun manualCloseGate(side: GateSide) {
        viewModelScope.launch {
            when (side) {
                GateSide.Left -> manualOpeningLeft = Date().time
                GateSide.Right -> manualOpeningRight = Date().time
            }
            gateRepository.setStatus(side, GateStatus.MANUAL_CLOSING)

        }
    }


    fun stopManual(side: GateSide) {
        viewModelScope.launch {
            val time = Date().time - when (side) {
                GateSide.Left -> manualOpeningLeft
                GateSide.Right -> manualOpeningRight
            }
            val message = SnackbarMessage(
                app.getString(R.string.movement_duration, time),
                SnackbarMessage.Action(
                    app.getString(R.string.save_time),
                    SaveTimeAction(gateRepository, side, time)
                )
            )
            messageChannel.send(message)
            gateRepository.setStatus(side, GateStatus.NOT_WORKING)
        }
    }

    fun openGate(side: GateSide) {
        viewModelScope.launch {
            gateRepository.setStatus(side, GateStatus.OPENING)

            val status = if (!side == GateSide.Right) _rightStatus.value
            else _leftStatus.value

            if (status != GateStatus.OPENED && status != GateStatus.CLOSED)
                gateRepository.setStatus(!side, GateStatus.NOT_WORKING)
        }
    }

    fun closeGate(side: GateSide) {
        viewModelScope.launch {
            gateRepository.setStatus(side, GateStatus.CLOSING)

            val status = if (!side == GateSide.Right) _rightStatus.value
            else _leftStatus.value

            if (status != GateStatus.OPENED && status != GateStatus.CLOSED)
                gateRepository.setStatus(!side, GateStatus.NOT_WORKING)
        }
    }

    fun setMovingTime(side: GateSide, time: Long) {
        viewModelScope.launch {
            gateRepository.setTime(side, time)
        }
    }

    fun stop(side: GateSide) {
        viewModelScope.launch {
            gateRepository.setStatus(side, GateStatus.NOT_WORKING)
        }
    }

    fun stop() {
        viewModelScope.launch {
            gateRepository.setStatus(GateSide.Right, GateStatus.NOT_WORKING)
            gateRepository.setStatus(GateSide.Left, GateStatus.NOT_WORKING)
        }
    }

    class SaveTimeAction(
        private val gateRepository: GateRepository,
        private val side: GateSide,
        private val time: Long
    ) : SnackbarAction {
        override suspend fun work() {
            gateRepository.setTime(side, time)
        }

    }
}

interface SnackbarAction {
    suspend fun work()
}

class SnackbarMessage(
    val message: String,
    val action: Action? = null
) {
    class Action(
        val message: String,
        val action: SnackbarAction
    )
}