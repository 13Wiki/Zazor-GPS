package com.gps.zazor.ui.base

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

abstract class BaseActivity<STATE : UiState, EVENT : UiEvent>(@LayoutRes private val layoutRes: Int) :
    AppCompatActivity() {

    abstract fun observeState(state: STATE?)

    abstract val viewModel: BaseViewModel<STATE, EVENT>

    /**
     * True for a screen that lays itself out under the system bars and places its own controls,
     * like the camera. Every other screen is kept clear of the bars and the keyboard here.
     */
    protected open val drawsUnderSystemBars: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
        if (!drawsUnderSystemBars) keepClearOfSystemBars()
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

    /**
     * From Android 15 an app targeting it is drawn edge to edge whatever the theme says, and the
     * window no longer shrinks for the keyboard: without this, titles sat under the status bar and
     * the OK button of the passcode screen under the keyboard. Opting in on every version gives
     * one behaviour to test instead of two.
     *
     * The keyboard is folded into the bottom inset, so a button pinned to the bottom rises above it.
     */
    private fun keepClearOfSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = maxOf(bars.bottom, keyboard.bottom)
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    protected open fun navigateTo(fragment: Fragment, container: Int, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().replace(container, fragment).apply {
            if (addToBackStack) addToBackStack(fragment::class.simpleName)
        }.commit()
    }
}
