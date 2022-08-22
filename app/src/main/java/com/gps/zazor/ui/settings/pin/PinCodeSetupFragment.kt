package com.gps.zazor.ui.settings.pin

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentPinSetupBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.pin.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

open class PinCodeSetupFragment : BaseFragment<PinCodeSetupContract.State, PinCodeSetupContract.Event>(R.layout.fragment_pin_setup) {

    override val viewModel by injectViewModel()

    open val errorRes: Int = R.string.pin_incorrect

    open val titleRes: Int = R.string.enter_pin

    private val binding by viewBinding(FragmentPinSetupBinding::bind)

    override fun observeState(state: PinCodeSetupContract.State?) {
        when (state) {
            is PinCodeSetupContract.State.CodeSet -> {
                requireActivity().finish()
            }
            is PinCodeSetupContract.State.CodeIncorrect -> {
                binding.tilText.error = getString(errorRes)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = getString(titleRes)
        binding.etText.doOnTextChanged { _, _, _, _ ->
            binding.tilText.error = null
        }
        binding.bOk.setOnClickListener {
            viewModel.sendEvent(PinCodeSetupContract.Event.CodeEntered(binding.etText.text.toString()))
        }
    }
}