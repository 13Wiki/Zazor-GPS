package com.gps.zazor.ui.base

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface BaseViewModel<STATE : UiState, EVENT : UiEvent> {

    val uiState: StateFlow<STATE?>

    val eventFlow: SharedFlow<EVENT?>

    fun init()

    fun sendEvent(event: EVENT?)

    fun reset()
}

abstract class BaseViewModelImpl<STATE : UiState, EVENT : UiEvent> : ViewModel(),
    BaseViewModel<STATE, EVENT> {

    override val uiState = MutableStateFlow<STATE?>(null)

    // A buffer keeps events sent before the collector is attached from being dropped.
    override val eventFlow = MutableSharedFlow<EVENT?>(extraBufferCapacity = EVENT_BUFFER)

    companion object {

        private const val EVENT_BUFFER = 64
    }

    protected abstract suspend fun initialState(): STATE?

    protected abstract fun onEventArrived(event: EVENT?)

    init {
        viewModelScope.launch {
            eventFlow.collect(::onEventArrived)
        }
    }

    @CallSuper
    override fun init() {
        launchIo {
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
        uiState.value = null
    }

    protected fun launch(block: suspend () -> Unit): Job =
        viewModelScope.launch { block() }

    /** Runs [block] off the main thread - use it for disk, database and network work. */
    protected fun launchIo(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit
    ): Job = viewModelScope.launch(dispatcher) { block() }
}
