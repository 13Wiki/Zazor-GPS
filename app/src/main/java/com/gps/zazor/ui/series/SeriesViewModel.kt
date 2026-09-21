package com.gps.zazor.ui.series

import com.gps.zazor.data.models.ApproachSeries
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface SeriesViewModel : BaseViewModel<SeriesContract.State, SeriesContract.Event>

/**
 * One approach series, read back from the photos that carry its id.
 *
 * Like an outing, a series is not stored as a thing of its own: delete its frames and it is gone,
 * with no second record left behind to forget about.
 */
class SeriesViewModelImpl(
    private val photoRepository: PhotoRepository
) : BaseViewModelImpl<SeriesContract.State, SeriesContract.Event>(), SeriesViewModel {

    private var seriesId: String? = null

    override suspend fun initialState(): SeriesContract.State = load()

    override fun onEventArrived(event: SeriesContract.Event?) {
        when (event) {
            is SeriesContract.Event.Load -> {
                seriesId = event.seriesId
                launchIo { uiState.value = load() }
            }
            is SeriesContract.Event.Reload -> launchIo { uiState.value = load() }
            else -> Unit
        }
    }

    private suspend fun load(): SeriesContract.State {
        val id = seriesId ?: return SeriesContract.State.Empty
        val series = ApproachSeries.from(photoRepository.getPhotos()).firstOrNull { it.id == id }
        return series?.let { SeriesContract.State.Content(it) } ?: SeriesContract.State.Empty
    }
}
