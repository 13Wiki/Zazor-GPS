package com.gps.zazor.ui.base

import android.os.Bundle
import androidx.activity.addCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

abstract class BaseActivity<STATE : UiState, EVENT : UiEvent>(@LayoutRes private val layoutRes: Int) :
    AppCompatActivity() {

    abstract fun observeState(state: STATE?)

    abstract val viewModel: BaseViewModel<STATE, EVENT>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
        // repeatOnLifecycle stops the collector while the activity is stopped instead of leaving
        // it running against a torn-down view hierarchy.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::observeState)
            }
        }
        registerBackHandling()
        viewModel.init()
    }

    /**
     * Back goes through the dispatcher, not `onBackPressed`: from Android 13 the predictive back
     * gesture never calls the override, so a screen relying on it simply stops responding.
     *
     * A listener returning false means it consumed the press, so the activity must not also pop.
     */
    private fun registerBackHandling() {
        onBackPressedDispatcher.addCallback(this) {
            val handled = supportFragmentManager.fragments.any {
                (it as? OnBackPressedListener)?.onBackPressed() == false
            }
            if (!handled) {
                // Step aside and let the next callback - ultimately the default finish - run.
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    protected open fun navigateTo(fragment: Fragment, container: Int, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().replace(container, fragment).apply {
            if (addToBackStack) addToBackStack(fragment::class.simpleName)
        }.commit()
    }
}
