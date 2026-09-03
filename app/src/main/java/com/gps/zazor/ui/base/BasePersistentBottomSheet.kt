package com.gps.zazor.ui.base

import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.gps.zazor.utils.extensions.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface PersistentBottomSheet {

    fun show()

    fun hide(): Boolean

    fun collapse(): Boolean
}

abstract class BasePersistentBottomSheet<STATE : UiState, EVENT : UiEvent> : PersistentBottomSheet {

    companion object {

        private const val COLLAPSED_PEEK_HEIGHT = 120
    }

    abstract val viewModel: BaseViewModel<STATE, EVENT>

    abstract val behavior: BottomSheetBehavior<*>

    abstract val view: View

    abstract fun observeState(state: STATE?)

    private var stateJob: Job? = null

    private var isInitialised = false

    private val sheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) onHidden()
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    @CallSuper
    override fun show() {
        if (behavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            onShown()
        }
    }

    @CallSuper
    override fun hide(): Boolean {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            onHidden()
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
            return true
        }
        return false
    }

    override fun collapse(): Boolean {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.peekHeight = COLLAPSED_PEEK_HEIGHT
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
    }

    /**
     * Wiring runs once per sheet instance.
     *
     * The old version re-registered the behaviour callback and started a fresh state collector on
     * every `show()`, so after a handful of photos each state update was handled several times
     * over and the collectors were never released.
     */
    @CallSuper
    protected open fun onShown() {
        if (!isInitialised) {
            isInitialised = true
            behavior.addBottomSheetCallback(sheetCallback)
            // The sheet's context is a themed wrapper, not the activity, so resolve the owner.
            view.context.findActivity()?.let { owner ->
                stateJob = owner.lifecycleScope.launch {
                    owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.uiState.collect(::observeState)
                    }
                }
            }
        }
        viewModel.init()
    }

    @CallSuper
    protected open fun onHidden() {
        viewModel.reset()
    }
}
