package com.gps.zazor.ui.settings.appearance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentAppearanceBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.appearance.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

/**
 * Icon and label on the home screen, plus a reminder of what the wipe code does.
 */
class AppearanceFragment : BaseFragment<AppearanceContract.State, AppearanceContract.Event>(
    R.layout.fragment_appearance
) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentAppearanceBinding::bind)

    private var hasChosen = false

    private val adapter by lazy {
        AppearanceAdapter {
            hasChosen = true
            viewModel.sendEvent(AppearanceContract.Event.Choose(it))
        }
    }

    override fun observeState(state: AppearanceContract.State?) {
        when (state) {
            is AppearanceContract.State.Content -> {
                if (adapter.current != state.current) {
                    adapter.current = state.current
                    // Only after a tap: opening the screen with a non-default icon already set is
                    // not a change and should say nothing.
                    if (hasChosen) {
                        Toast.makeText(requireContext(), R.string.appearance_changed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                binding.tvWipeCode.setText(
                    if (state.hasWipeCode) R.string.wipe_code_set else R.string.wipe_code_not_set
                )
            }
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAppearances.adapter = adapter
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
