package com.gps.zazor.ui.settings.appearance

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentAppearanceBinding
import com.gps.zazor.ui.auth.pin.PIN_LENGTH
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.SettingsCallback
import com.gps.zazor.ui.settings.appearance.di.injectViewModel
import com.gps.zazor.utils.launcher.LauncherAppearance
import com.gps.zazor.utils.viewBinding.viewBinding

/**
 * Icon and label on the home screen, and the code that empties the app.
 */
class AppearanceFragment : BaseFragment<AppearanceContract.State, AppearanceContract.Event>(
    R.layout.fragment_appearance
) {

    private companion object {

        /** The design spaces the code's dots nine across, matching their size. */
        const val DOT_GAP_DP = 9
    }

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentAppearanceBinding::bind)

    private var hasChosen = false

    private val adapter by lazy {
        AppearanceAdapter(::choose)
    }

    override fun observeState(state: AppearanceContract.State?) {
        when (state) {
            is AppearanceContract.State.Content -> render(state)
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAppearances.adapter = adapter
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.tvLabelValue.setOnClickListener { chooseLabel() }
        binding.clWipeCode.setOnClickListener {
            (activity as? SettingsCallback)?.openClearCodeSetup()
        }
    }

    override fun onResume() {
        super.onResume()
        // The wipe code may have been set on the screen this one opened.
        viewModel.init()
    }

    private fun render(state: AppearanceContract.State.Content) {
        if (adapter.current != state.current) {
            adapter.current = state.current
            // Only after a tap: opening the screen with a non-default icon already set is not a
            // change and should say nothing.
            if (hasChosen) {
                Toast.makeText(requireContext(), R.string.appearance_changed, Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.tvLabelValue.setText(state.current.title)
        binding.tvWipeCode.setText(
            if (state.hasWipeCode) R.string.wipe_code_set else R.string.wipe_code_not_set
        )
        showWipeDots(state.hasWipeCode)
    }

    /**
     * How long the code is, never what it is. With no code set there is nothing to show, and the
     * row reads as the invitation to set one.
     */
    private fun showWipeDots(hasCode: Boolean) {
        binding.llWipeDots.isVisible = hasCode
        if (!hasCode || binding.llWipeDots.childCount == PIN_LENGTH) return
        val size = resources.getDimensionPixelSize(R.dimen.ds_wipe_dot)
        val gap = (DOT_GAP_DP * resources.displayMetrics.density).toInt()
        binding.llWipeDots.removeAllViews()
        repeat(PIN_LENGTH) { index ->
            binding.llWipeDots.addView(
                ImageView(requireContext()).apply {
                    setBackgroundResource(R.drawable.ds_wipe_dot)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        if (index > 0) marginStart = gap
                    }
                }
            )
        }
    }

    /**
     * The label is whichever alias is enabled, so picking a label picks an icon. Offered from the
     * label's own row because that is where someone looks for it.
     */
    private fun chooseLabel() {
        val options = LauncherAppearance.Appearance.entries
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.appearance_label_header)
            .setItems(options.map { getString(it.title) }.toTypedArray()) { _, index ->
                choose(options[index])
            }
            .setNegativeButton(R.string.cancel_action, null)
            .show()
    }

    private fun choose(appearance: LauncherAppearance.Appearance) {
        hasChosen = true
        viewModel.sendEvent(AppearanceContract.Event.Choose(appearance))
    }
}
