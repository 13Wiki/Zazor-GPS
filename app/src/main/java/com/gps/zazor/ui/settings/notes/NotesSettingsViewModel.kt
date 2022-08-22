package com.gps.zazor.ui.settings.notes

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface NotesSettingsViewModel : BaseViewModel<NotesSettingsContract.State, NotesSettingsContract.Event>

class NotesSettingsViewModelImpl(private val prefs: AppPreferences) : BaseViewModelImpl<NotesSettingsContract.State, NotesSettingsContract.Event>(), NotesSettingsViewModel {

    override suspend fun initialState(): NotesSettingsContract.State =
        NotesSettingsContract.State.Initial(
            prefs.isDisplayCoordinates(),
            prefs.isDisplayDate(),
            prefs.isDisplayTime(),
            prefs.isDisplayAccuracy()
        )

    override fun onEventArrived(event: NotesSettingsContract.Event?) {
        when (event) {
            is NotesSettingsContract.Event.CoordinatesSwitched -> prefs.putDisplayCoordinates(event.isChecked)
            is NotesSettingsContract.Event.DateSwitched -> prefs.putDisplayDate(event.isChecked)
            is NotesSettingsContract.Event.TimeSwitched -> prefs.putDisplayTime(event.isChecked)
            is NotesSettingsContract.Event.AccuracySwitched -> prefs.putDisplayAccuracy(event.isChecked)
        }
    }
}