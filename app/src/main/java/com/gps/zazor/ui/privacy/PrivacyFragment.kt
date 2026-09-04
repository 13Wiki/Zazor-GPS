package com.gps.zazor.ui.privacy

import android.content.Context
import android.os.Bundle
import android.view.View
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentPrivacyBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.privacy.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

/**
 * What the app never collects, and the switch to refuse what it does.
 *
 * Serves two hosts: the first run, where accepting moves on, and Settings, where it is a
 * reference and the button simply goes back.
 */
class PrivacyFragment : BaseFragment<PrivacyContract.State, PrivacyContract.Event>(
    R.layout.fragment_privacy
) {

    /** Implemented by the launch screen; absent when opened from Settings. */
    interface Host {

        fun onPrivacyAccepted()
    }

    companion object {

        private const val ARG_FIRST_RUN = "firstRun"

        fun newInstance(isFirstRun: Boolean) = PrivacyFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_FIRST_RUN, isFirstRun) }
        }
    }

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentPrivacyBinding::bind)

    /**
     * Resolved from the host rather than assigned as a lambda: a field set from outside is lost
     * when the fragment is recreated, which used to strand the person on the first-run screen
     * after a rotation.
     */
    private var host: Host? = null

    private val isFirstRun: Boolean
        get() = arguments?.getBoolean(ARG_FIRST_RUN, true) ?: true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        host = context as? Host
    }

    override fun onDetach() {
        host = null
        super.onDetach()
    }

    override fun observeState(state: PrivacyContract.State?) {
        when (state) {
            is PrivacyContract.State.Content -> binding.swAnalytics.isChecked = state.analyticsEnabled
            is PrivacyContract.State.Accepted -> finish()
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvAccept.setText(if (isFirstRun) R.string.privacy_accept else R.string.done)
        binding.swAnalytics.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) {
                viewModel.sendEvent(PrivacyContract.Event.ToggleAnalytics(checked))
            }
        }
        binding.tvAccept.setOnClickListener {
            if (isFirstRun) viewModel.sendEvent(PrivacyContract.Event.Accept) else finish()
        }
    }

    private fun finish() {
        if (isFirstRun) {
            host?.onPrivacyAccepted()
        } else {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
