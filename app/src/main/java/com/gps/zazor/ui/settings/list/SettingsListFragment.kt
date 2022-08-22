package com.gps.zazor.ui.settings.list

import android.content.Context
import android.os.Bundle
import android.view.View
import com.gps.zazor.R
import com.gps.zazor.data.models.MainSettingType
import com.gps.zazor.databinding.FragmentSettingsListBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.SettingsCallback
import com.gps.zazor.ui.settings.list.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

class SettingsListFragment : BaseFragment<SettingsListContract.State, SettingsListContract.Event>(R.layout.fragment_settings_list) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentSettingsListBinding::bind)

    private var settingsCallback: SettingsCallback? = null

    override fun observeState(state: SettingsListContract.State?) {
        when (state) {
            is SettingsListContract.State.Initial -> {
                binding.rvSettings.adapter = SettingsListAdapter(state.settings) { type ->
                    when (type) {
                        MainSettingType.PIN_CODE -> settingsCallback?.openPinSetup()
                        MainSettingType.CLEAR_CODE -> settingsCallback?.openClearCodeSetup()
                        MainSettingType.NOTES -> settingsCallback?.openNotesSettings()
                        MainSettingType.TRIAL_CODE -> settingsCallback?.openTrialCode()
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingsCallback = activity as? SettingsCallback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    override fun onDetach() {
        settingsCallback = null
        super.onDetach()
    }
}