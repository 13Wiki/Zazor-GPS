package com.gps.zazor.ui.settings.appearance

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.launcher.LauncherAppearance

interface AppearanceViewModel : BaseViewModel<AppearanceContract.State, AppearanceContract.Event>

class AppearanceViewModelImpl(
    private val launcherAppearance: LauncherAppearance,
    private val prefs: AppPreferences
) : BaseViewModelImpl<AppearanceContract.State, AppearanceContract.Event>(), AppearanceViewModel {

    override suspend fun initialState(): AppearanceContract.State =
        AppearanceContract.State.Content(launcherAppearance.current(), prefs.getClearCode() != null)

    override fun onEventArrived(event: AppearanceContract.Event?) {
        when (event) {
            // Each alias costs a blocking binder call, and switching touches all four.
            is AppearanceContract.Event.Choose -> launchIo {
                launcherAppearance.apply(event.appearance)
                uiState.value = AppearanceContract.State.Content(
                    launcherAppearance.current(),
                    prefs.getClearCode() != null
                )
            }
            else -> Unit
        }
    }
}
