package com.gps.zazor.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.NO_ID
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gps.zazor.ui.photo.PhotoCallback
import kotlinx.coroutines.launch

abstract class BaseFragment<STATE : UiState, EVENT : UiEvent>(@LayoutRes private val layoutRes: Int) :
    Fragment(), OnBackPressedListener {

    abstract val viewModel: BaseViewModel<STATE, EVENT>

    open val screenTitle: Int? = null

    protected var callback: PhotoCallback? = null

    abstract fun observeState(state: STATE?)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? PhotoCallback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.takeUnless { layoutRes == NO_ID }?.inflate(layoutRes, container, false)

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // The collector is tied to the *view* lifecycle: the previous code used the fragment
        // lifecycle, so after the view was destroyed the collector kept touching dead bindings.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::observeState)
            }
        }
        viewModel.init()
    }

    override fun onDetach() {
        callback = null
        super.onDetach()
    }

    override fun onBackPressed(): Boolean =
        childFragmentManager.fragments.takeIf { it.isNotEmpty() }?.all {
            (it as? OnBackPressedListener)?.onBackPressed() ?: true
        } ?: true

    protected open fun navigateTo(fragment: Fragment, container: Int, addToBackStack: Boolean = false) {
        childFragmentManager.beginTransaction().replace(container, fragment).apply {
            if (addToBackStack) addToBackStack(fragment::class.simpleName)
        }.commit()
    }
}
