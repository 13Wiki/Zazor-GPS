package com.gps.zazor.ui.base

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface BaseViewModel<STATE : UiState, EVENT : UiEvent> {

    val uiState: StateFlow<STATE?>

    val eventFlow : SharedFlow<EVENT?>

    fun init()

    fun sendEvent(event: EVENT?)

    fun reset()
}

abstract class BaseViewModelImpl<STATE : UiState, EVENT : UiEvent> : ViewModel(), BaseViewModel<STATE, EVENT> {

    override val uiState = MutableStateFlow<STATE?>(null)

    override val eventFlow = MutableSharedFlow<EVENT?>()

    protected abstract suspend fun initialState(): STATE?

    protected abstract fun onEventArrived(event: EVENT?)

    init {
        viewModelScope.launch {
            eventFlow.collect(::onEventArrived)
        }
    }

    @CallSuper
    override fun init() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value = initialState()
        }
    }

    @CallSuper
    override fun sendEvent(event: EVENT?) {
        viewModelScope.launch {
            eventFlow.emit(event)
        }
    }

    @CallSuper
    override fun reset() {
        sendEvent(null)
        uiState.value = null
    }

    protected fun launch(remoteCall: suspend () -> Unit) =
        viewModelScope.launch {
            remoteCall()
        }
}