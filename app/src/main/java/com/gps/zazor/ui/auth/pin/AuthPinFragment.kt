package com.gps.zazor.ui.auth.pin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentAuthPinBinding
import com.gps.zazor.ui.auth.pin.di.injectViewModel
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoActivity
import com.gps.zazor.utils.extensions.hasBiometric
import com.gps.zazor.utils.viewBinding.viewBinding

class AuthPinFragment : BaseFragment<AuthPinContract.State, AuthPinContract.Event>(R.layout.fragment_auth_pin){

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentAuthPinBinding::bind)

    override fun observeState(state: AuthPinContract.State?) {
        when (state) {
            is AuthPinContract.State.AuthSuccess -> {
                startActivity(PhotoActivity.newIntent(requireContext()))
                activity?.finish()
            }
            is AuthPinContract.State.AuthFailure -> {
                binding.tilText.error = getString(R.string.auth_error)
            }
            AuthPinContract.State.DataCleared -> {
                Toast.makeText(requireContext(), getString(R.string.data_was_cleared), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivFingerprint.isVisible = requireContext().hasBiometric()
        binding.etText.doOnTextChanged { text, _, _, _ ->
            binding.tilText.error = null
            viewModel.sendEvent(AuthPinContract.Event.PinEntered(text.toString()))
        }
        binding.ivFingerprint.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun setBiometricPromptInfo(
        title: String,
        subtitle: String,
        description: String
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        // Use Device Credentials if allowed, otherwise show Cancel Button
        builder.apply {
            setDeviceCredentialAllowed(true)
        }

        return builder.build()
    }

    private fun initBiometricPrompt(onSuccess: () -> Unit): BiometricPrompt {
        // 1
        val executor = ContextCompat.getMainExecutor(activity)

        // 2
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = Unit

            override fun onAuthenticationFailed() = Unit

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                onSuccess()
        }

        // 3
        return BiometricPrompt(requireActivity(), executor, callback)
    }

    private fun showBiometricPrompt(
        title: String = getString(R.string.bio_auth_title),
        subtitle: String = getString(R.string.bio_auth_subtitle),
        description: String = getString(R.string.bio_auth_desc),
        cryptoObject: BiometricPrompt.CryptoObject? = null
    ) {
        // 1
        val promptInfo = setBiometricPromptInfo(
            title,
            subtitle,
            description
        )

        // 2
        val biometricPrompt = initBiometricPrompt {
            startActivity(PhotoActivity.newIntent(requireContext()))
            activity?.finish()
        }

        // 3
        biometricPrompt.apply {
            if (cryptoObject == null) authenticate(promptInfo)
            else authenticate(promptInfo, cryptoObject)
        }
    }
}