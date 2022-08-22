package com.gps.zazor.ui.settings.trial

import android.os.Bundle
import android.view.View
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentTrialBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.trial.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

class TrialCodeFragment : BaseFragment<TrialCodeContract.State, TrialCodeContract.Event>(R.layout.fragment_trial) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentTrialBinding::bind)

    override fun observeState(state: TrialCodeContract.State?) {
        when (state) {
            is TrialCodeContract.State.CodeSet -> requireActivity().finish()
            is TrialCodeContract.State.CodeIncorrect -> {
                binding.tilText.error = getString(R.string.code_trial_incorrect)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bOk.setOnClickListener {
            viewModel.sendEvent(TrialCodeContract.Event.CodeEntered(binding.etText.text.toString()))
        }
    }
}