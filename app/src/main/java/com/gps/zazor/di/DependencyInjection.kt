package com.gps.zazor.di

import android.app.Application
import com.gps.zazor.data.di.prefsModule
import com.gps.zazor.data.di.repositoriesModule
import com.gps.zazor.ui.auth.di.authModule
import com.gps.zazor.ui.auth.pin.di.authPinModule
import com.gps.zazor.ui.photo.di.photoModule
import com.gps.zazor.ui.media.di.mediaModule
import com.gps.zazor.ui.media.edit.di.editMediaModule
import com.gps.zazor.ui.media.list.di.mediaListModule
import com.gps.zazor.ui.outings.di.outingsModule
import com.gps.zazor.ui.privacy.di.privacyModule
import com.gps.zazor.ui.settings.appearance.di.appearanceModule
import com.gps.zazor.ui.share.di.shareModule
import com.gps.zazor.ui.photo.base.di.basicPhotoModule
import com.gps.zazor.ui.photo.collage.container.di.collageContainerModule
import com.gps.zazor.ui.photo.collage.di.collageModule
import com.gps.zazor.ui.photo.collage.photo.di.collagePhotoModule
import com.gps.zazor.ui.settings.clearCode.clearCodeSetupModule
import com.gps.zazor.ui.settings.di.settingsModule
import com.gps.zazor.ui.settings.list.di.settingsListModule
import com.gps.zazor.ui.settings.notes.di.notesSettingsModule
import com.gps.zazor.ui.settings.pin.di.pinCodeSetupModule
import com.gps.zazor.ui.settings.trial.di.trialCodeModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

object DependencyInjection {

    fun init(application: Application) {
        startKoin {
            androidContext(application)
            modules(prefsModule, repositoriesModule, authModule, authPinModule, photoModule, collagePhotoModule,
                basicPhotoModule, collageContainerModule,
                collageModule, mediaModule, mediaListModule, outingsModule, shareModule, privacyModule, appearanceModule, editMediaModule, settingsModule, settingsListModule,
                pinCodeSetupModule, clearCodeSetupModule, notesSettingsModule, trialCodeModule)
        }
    }
}