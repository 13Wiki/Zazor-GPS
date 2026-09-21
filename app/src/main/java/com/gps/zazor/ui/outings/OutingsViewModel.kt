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
import java.time.LocalDate
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

    /** A day asked for before the outings were loaded; applied by the first load that sees it. */
    private var pendingDate: LocalDate? = null

    override suspend fun initialState(): OutingsContract.State = load()

    override fun onEventArrived(event: OutingsContract.Event?) {
        when (event) {
            is OutingsContract.Event.Reload -> launchIo { uiState.value = load() }
            is OutingsContract.Event.SelectOuting -> {
                selected = event.outing
                uiState.value = (uiState.value as? OutingsContract.State.Content)
                    ?.copy(selected = event.outing)
            }
            is OutingsContract.Event.SelectDate -> selectDate(event.date)
            is OutingsContract.Event.DeleteOuting -> deleteOuting(event.outing)
            is OutingsContract.Event.DeleteAll -> deleteAll()
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
        // A day asked for before the first load wins once; then the usual rule takes over.
        val wanted = pendingDate?.also { pendingDate = null } ?: selected?.date
        // Keep the open day selected across a reload; fall back to the most recent one.
        selected = outings.firstOrNull { it.date == wanted } ?: outings.firstOrNull()
        return OutingsContract.State.Content(outings, selected)
    }

    private fun deleteAll() {
        launchIo {
            val content = uiState.value as? OutingsContract.State.Content ?: return@launchIo
            val photos = content.outings.flatMap { it.photos }
            photos.forEach { photoRepository.deletePhoto(it) }
            selected = null
            uiState.value = load()
            effectFlow.emit(OutingsContract.Effect.Deleted(photos.size))
        }
    }

    /**
     * Chosen by date rather than by the object, because the screen that asks for a day may not
     * have the loaded outings yet. A day with nothing in it leaves the selection alone.
     */
    private fun selectDate(date: LocalDate) {
        val content = uiState.value as? OutingsContract.State.Content
        if (content == null) {
            // Asked for before the first load finished: remember it for when the list arrives.
            pendingDate = date
            return
        }
        val outing = content.outings.firstOrNull { it.date == date } ?: return
        selected = outing
        uiState.value = content.copy(selected = outing)
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
