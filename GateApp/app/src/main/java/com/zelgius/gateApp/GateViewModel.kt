package com.zelgius.gateApp

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // as it's the application context, there is no leak using it in a viewModel
class GateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val _gateRepository: GateRepository? = null // Compose previous purpose
) : ViewModel() {
    private val gateRepository: GateRepository
        get() = _gateRepository!!

    companion object {
        const val FAVORITE_SIDE = "FAVORITE_SIDE"
    }

    private val messageChannel = Channel<SnackbarMessage>()
    val messageFlow = messageChannel.receiveAsFlow()

    private val sharedPreferences = context.getSharedPreferences("Default", Context.MODE_PRIVATE)

    private val _timeRight = MutableLiveData(0L)
    val timeRight: LiveData<Long>
        get() = _timeRight

    private val _timeLeft = MutableLiveData(0L)
    val timeLeft: LiveData<Long>
        get() = _timeLeft

    private val _leftStatus = MutableLiveData(GateStatus.NOT_WORKING)
    val leftStatus: LiveData<GateStatus>
        get() = _leftStatus

    private val _rightStatus = MutableLiveData(GateStatus.NOT_WORKING)
    val rightStatus: LiveData<GateStatus>
        get() = _rightStatus

    private val _rightProgress = MutableLiveData<Int?>()
    val rightProgress: LiveData<Int?>
        get() = _rightProgress

    private val _leftProgress = MutableLiveData<Int?>()
    val leftProgress: LiveData<Int?>
        get() = _leftProgress

    private val _favorite = MutableLiveData(GateSide.Left)
    val favorite: LiveData<GateSide>
        get() = _favorite

    private var manualOpeningLeft = 0L
    private var manualOpeningRight = 0L

    init {
        viewModelScope.launch {

            _gateRepository?.listenStatus(GateSide.Left) { status -> _leftStatus.postValue(status) }
            _gateRepository?.listenStatus(GateSide.Right) { status -> _rightStatus.postValue(status) }

            _gateRepository?.listenProgress(GateSide.Left) { _leftProgress.postValue(it) }
            _gateRepository?.listenProgress(GateSide.Right) { _rightProgress.postValue(it) }

            _gateRepository?.listenTime { side, time ->
                when (side) {
                    GateSide.Left -> _timeLeft.postValue(time)
                    GateSide.Right -> _timeRight.postValue(time)
                }
            }

            _favorite.value = sharedPreferences.getString(
                FAVORITE_SIDE, null
            ).let {
                GateSide.values().find { side -> side.id == it }?: GateSide.Left
            }
        }
    }

    fun openGate() {
        viewModelScope.launch {
            GateOpeningService.startForeground(context, Direction.Open)
        }
    }

    fun closeGate() {
        viewModelScope.launch {
            GateOpeningService.startForeground(context, Direction.Close)
        }
    }

    fun toggleFavorite(side: GateSide) {
        viewModelScope.launch {
            sharedPreferences.edit {
                putString(FAVORITE_SIDE, side.id)
            }
            _favorite.value = side
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
                context.getString(R.string.movement_duration, time),
                SnackbarMessage.Action(
                    context.getString(R.string.save_time),
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