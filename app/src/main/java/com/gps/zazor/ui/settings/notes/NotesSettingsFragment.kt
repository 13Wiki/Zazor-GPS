package com.gps.zazor.ui.settings.notes

import android.os.Bundle
import android.view.View
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentNotesSettingsBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.notes.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

class NotesSettingsFragment:
    BaseFragment<NotesSettingsContract.State, NotesSettingsContract.Event>(R.layout.fragment_notes_settings) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentNotesSettingsBinding::bind)

    override fun observeState(state: NotesSettingsContract.State?) {
        when (state) {
            is NotesSettingsContract.State.Initial -> {
                binding.sCoordinates.isChecked = state.displayCoordinates
                binding.sDate.isChecked = state.displayDate
                binding.sTime.isChecked = state.displayTime
                binding.sAccuracy.isChecked = state.displayAccuracy
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sCoordinates.setOnCheckedChangeListener { _, isChecked ->
            viewModel.sendEvent(NotesSettingsContract.Event.CoordinatesSwitched(isChecked))
        }
        binding.sDate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.sendEvent(NotesSettingsContract.Event.DateSwitched(isChecked))
        }
        binding.sTime.setOnCheckedChangeListener { _, isChecked ->
            viewModel.sendEvent(NotesSettingsContract.Event.TimeSwitched(isChecked))
        }
        binding.sAccuracy.setOnCheckedChangeListener { _, isChecked ->
            viewModel.sendEvent(NotesSettingsContract.Event.AccuracySwitched(isChecked))
        }
    }
}