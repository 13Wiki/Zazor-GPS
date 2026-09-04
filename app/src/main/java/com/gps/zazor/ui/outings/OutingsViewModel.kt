package com.gps.zazor.ui.outings

import com.gps.zazor.data.models.Outing
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.export.TrackFileWriter
import com.gps.zazor.utils.export.TrackFormat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.format.DateTimeFormatter

interface OutingsViewModel : BaseViewModel<OutingsContract.State, OutingsContract.Event> {

    val effects: SharedFlow<OutingsContract.Effect>
}

class OutingsViewModelImpl(
    private val photoRepository: PhotoRepository,
    private val trackFileWriter: TrackFileWriter
) : BaseViewModelImpl<OutingsContract.State, OutingsContract.Event>(), OutingsViewModel {

    private val effectFlow = MutableSharedFlow<OutingsContract.Effect>(extraBufferCapacity = 8)

    override val effects: SharedFlow<OutingsContract.Effect> = effectFlow.asSharedFlow()

    private var selected: Outing? = null

    override suspend fun initialState(): OutingsContract.State = load()

    override fun onEventArrived(event: OutingsContract.Event?) {
        when (event) {
            is OutingsContract.Event.Reload -> launchIo { uiState.value = load() }
            is OutingsContract.Event.SelectOuting -> {
                selected = event.outing
                uiState.value = (uiState.value as? OutingsContract.State.Content)
                    ?.copy(selected = event.outing)
            }
            is OutingsContract.Event.DeleteOuting -> deleteOuting(event.outing)
            is OutingsContract.Event.ExportOuting -> exportOuting(event.outing, event.format)
            else -> Unit
        }
    }

    /**
     * Outings are computed from the photos, never stored: deleting a day's photos removes that
     * day's track by construction, with no second place to forget to clean.
     */
    private suspend fun load(): OutingsContract.State.Content {
        val outings = Outing.from(photoRepository.getPhotos())
        // Keep the open day selected across a reload; fall back to the most recent one.
        selected = outings.firstOrNull { it.date == selected?.date } ?: outings.firstOrNull()
        return OutingsContract.State.Content(outings, selected)
    }

    private fun deleteOuting(outing: Outing) {
        launchIo {
            outing.photos.forEach { photoRepository.deletePhoto(it) }
            if (selected?.date == outing.date) selected = null
            uiState.value = load()
            effectFlow.emit(OutingsContract.Effect.Deleted(outing.photos.size))
        }
    }

    private fun exportOuting(outing: Outing, format: TrackFormat) {
        launchIo {
            val name = "Zazor " + outing.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            val file = trackFileWriter.write(outing.photos, format, name)
            effectFlow.emit(
                file?.let { OutingsContract.Effect.Exported(it, format) }
                    ?: OutingsContract.Effect.ExportFailed
            )
        }
    }
}
