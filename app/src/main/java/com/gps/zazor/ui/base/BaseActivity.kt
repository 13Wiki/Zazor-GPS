package com.gps.zazor.ui.base

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class BaseActivity<STATE : UiState, EVENT : UiEvent>(@LayoutRes private val layoutRes: Int) : FragmentActivity() {

    abstract fun observeState(state: STATE?)

    abstract val viewModel : BaseViewModel<STATE, EVENT>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
        lifecycleScope.launch {
            viewModel.init()
            viewModel.uiState.collect(::observeState)
        }
    }

    override fun onPause() {
        resetFlows()
        super.onPause()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.any {
                (it as? OnBackPressedListener)?.onBackPressed() == true }) {
            super.onBackPressed()
        }
    }

    protected open fun navigateTo(fragment: Fragment, container: Int, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().replace(container, fragment).apply {
            if (addToBackStack) addToBackStack(fragment::class.simpleName)
        }.commit()
    }

    private fun showNoInternetError() {
        Toast.makeText(this, "Нет соединения", Toast.LENGTH_SHORT).show()
    }

    private fun resetFlows() {
        viewModel.reset()
    }
}