package com.zelgius.gateApp

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.zelgius.gateApp.service.Direction
import com.zelgius.gateApp.service.GateOpeningService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // as it's the application context, there is no leak using it in a viewModel
class GateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gateRepository: GateRepository // Compose previous purpose
) : ViewModel() {
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

    private val listeners = mutableListOf<ListenerRegistration?>()


    private val _lightIsOn = MutableLiveData(false)
    val lightIsOn: LiveData<Boolean>
        get() = _lightIsOn

    private val _lightTime = MutableLiveData(0L)

    val lightTime: LiveData<Long>
        get() = _lightTime

    private var manualOpeningLeft = 0L
    private var manualOpeningRight = 0L


    init {
        viewModelScope.launch {

            gateRepository.listenStatus(GateSide.Left) { status -> _leftStatus.postValue(status) }
            gateRepository.listenStatus(GateSide.Right) { status -> _rightStatus.postValue(status) }

            gateRepository.listenProgress(GateSide.Left) { _leftProgress.postValue(it) }
            gateRepository.listenProgress(GateSide.Right) { _rightProgress.postValue(it) }

            launch {
                gateRepository.flowLightStatus().let { (l, flow) ->
                    listeners.add(l)
                    flow.collectLatest {
                        _lightIsOn.postValue(it)
                    }
                }
            }

            launch {
                gateRepository.flowLightTime().let { (l, flow) ->
                    listeners.add(l)
                    flow.collectLatest {
                        _lightTime.postValue(it)
                    }
                }
            }

            launch {
                gateRepository.flowFavorite().let { (l, flow) ->
                    listeners.add(l)

                    flow.first().let {
                        if(it == null) gateRepository.setFavorite(
                            sharedPreferences.getString(
                                FAVORITE_SIDE, null
                            ).let {
                                GateSide.entries.find { side -> side.id == it } ?: GateSide.Left
                            }
                        )
                    }
                    flow.filterNotNull().collectLatest {
                        _favorite.postValue(it)
                    }
                }
            }

            gateRepository.listenTime { side, time ->
                when (side) {
                    GateSide.Left -> _timeLeft.postValue(time)
                    GateSide.Right -> _timeRight.postValue(time)
                }
            }
        }
    }
    override fun onCleared() {
        listeners.forEach {
            it?.remove()
        }
        super.onCleared()
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
            gateRepository.setFavorite(side)
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

    fun showSnackBar(message: String, action: Pair<String, () -> Unit>? = null) {
        viewModelScope.launch {
            messageChannel.send(SnackbarMessage(message, action?.let { (text, action) ->
                SnackbarMessage.Action(
                    text,
                    object : SnackbarAction {
                        override suspend fun work() {
                            action()
                        }
                    }
                )
            }
            ))
        }
    }

    private fun lightDuration(side: GateSide) = when (side) {
        GateSide.Left -> timeLeft.value ?: 0L
        GateSide.Right -> timeRight.value ?: 0L
    } + (lightTime.value ?: 0L) * 1000L

    fun openGate(side: GateSide) {
        viewModelScope.launch {
            gateRepository.setStatus(side, GateStatus.OPENING)
            gateRepository.openLightForDuration(lightDuration(side))

            val status = if (!side == GateSide.Right) _rightStatus.value
            else _leftStatus.value

            if (status != GateStatus.OPENED && status != GateStatus.CLOSED)
                gateRepository.setStatus(!side, GateStatus.NOT_WORKING)

        }
    }

    fun closeGate(side: GateSide) {
        viewModelScope.launch {
            gateRepository.setStatus(side, GateStatus.CLOSING)
            gateRepository.openLightForDuration(lightDuration(side))

            val status = if (!side == GateSide.Right) _rightStatus.value
            else _leftStatus.value

            if (status != GateStatus.OPENED && status != GateStatus.CLOSED)
                gateRepository.setStatus(!side, GateStatus.NOT_WORKING)
        }
    }

    fun setStatus(side: GateSide, status: GateStatus) {
        viewModelScope.launch {
            gateRepository.setStatus(side, status)
            gateRepository.setCurrentStatus(side, status)
            gateRepository.setProgress(side, 100)
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

    fun toggleLight(isOn: Boolean) {
        viewModelScope.launch {
            gateRepository.setLightStatus(isOn)
        }
    }

    fun setLightTime(time: Long) {
        viewModelScope.launch {
            gateRepository.setLightTime(time)
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