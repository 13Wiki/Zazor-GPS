package com.gps.zazor.ui.share

import android.content.Context
import com.gps.zazor.R
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.export.BundleWriter
import com.gps.zazor.utils.export.ReportBuilder
import com.gps.zazor.utils.time.PhotoClock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface ShareViewModel : BaseViewModel<ShareContract.State, ShareContract.Event> {

    val effects: SharedFlow<ShareContract.Effect>
}

class ShareViewModelImpl(
    private val context: Context,
    private val photoRepository: PhotoRepository,
    private val bundleWriter: BundleWriter
) : BaseViewModelImpl<ShareContract.State, ShareContract.Event>(), ShareViewModel {

    private val effectFlow = MutableSharedFlow<ShareContract.Effect>(extraBufferCapacity = 8)

    override val effects: SharedFlow<ShareContract.Effect> = effectFlow.asSharedFlow()

    private var photos: List<Photo> = emptyList()

    private var options = ShareContract.Options()

    override suspend fun initialState(): ShareContract.State =
        ShareContract.State.Content(photos, options)

    override fun onEventArrived(event: ShareContract.Event?) {
        when (event) {
            is ShareContract.Event.Load -> load(event.paths)
            is ShareContract.Event.ToggleCoordinates -> update { copy(coordinates = event.on) }
            is ShareContract.Event.ToggleTrack -> update { copy(track = event.on) }
            is ShareContract.Event.ToggleVoiceNotes -> update { copy(voiceNotes = event.on) }
            is ShareContract.Event.SendPhotos -> sendPhotos()
            is ShareContract.Event.SendBundle -> sendBundle()
            is ShareContract.Event.SendReport -> sendReport()
            else -> Unit
        }
    }

    private fun load(paths: List<String>) {
        launchIo {
            photos = paths.mapNotNull { photoRepository.getPhoto(it) }
            uiState.value = ShareContract.State.Content(photos, options)
        }
    }

    private fun update(change: ShareContract.Options.() -> ShareContract.Options) {
        options = options.change()
        uiState.value = ShareContract.State.Content(photos, options)
    }

    private fun sendPhotos() {
        launchIo {
            effectFlow.emit(
                if (photos.isEmpty()) ShareContract.Effect.Empty
                else ShareContract.Effect.SharePhotos(photos)
            )
        }
    }

    private fun sendBundle() {
        launchIo {
            if (photos.isEmpty()) {
                effectFlow.emit(ShareContract.Effect.Empty)
                return@launchIo
            }
            uiState.value = ShareContract.State.Content(photos, options, isPreparing = true)
            val payload = if (options.voiceNotes) photos else photos.map { it.copy(voiceNotePath = null) }
            val file = bundleWriter.write(
                photos = payload,
                bundleName = context.getString(R.string.app_name) + " " +
                    PhotoClock.formatDate(PhotoClock.now()),
                report = ReportBuilder.build(context, photos, options.coordinates),
                includeTrack = options.track
            )
            uiState.value = ShareContract.State.Content(photos, options, isPreparing = false)
            effectFlow.emit(
                file?.let { ShareContract.Effect.ShareBundle(it) } ?: ShareContract.Effect.Failed
            )
        }
    }

    private fun sendReport() {
        launchIo {
            if (photos.isEmpty()) {
                effectFlow.emit(ShareContract.Effect.Empty)
                return@launchIo
            }
            effectFlow.emit(
                ShareContract.Effect.ShareText(
                    ReportBuilder.build(context, photos, options.coordinates)
                )
            )
        }
    }
}
