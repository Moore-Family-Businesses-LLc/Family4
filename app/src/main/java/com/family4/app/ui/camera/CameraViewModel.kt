package com.family4.app.ui.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraViewModel : ViewModel() {

    private val timerOptions = listOf(0, 3, 5, 10)
    private var timerIndex = 0

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    fun cycleTimer() {
        timerIndex = (timerIndex + 1) % timerOptions.size
        _timerSeconds.value = timerOptions[timerIndex]
    }
}
