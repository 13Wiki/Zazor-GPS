package com.gps.zazor.ui.series

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.gps.zazor.R
import com.gps.zazor.data.models.ApproachSeries
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.databinding.FragmentSeriesBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.MediaCallback
import com.gps.zazor.ui.series.di.injectViewModel
import com.gps.zazor.utils.FragmentArgumentDelegate
import com.gps.zazor.utils.viewBinding.viewBinding
import org.koin.android.ext.android.inject

/**
 * One approach series: the frames from the wide shot in to the spot, and which of them the
 * coordinate comes from.
 */
class SeriesFragment : BaseFragment<SeriesContract.State, SeriesContract.Event>(
    R.layout.fragment_series
) {

    companion object {

        fun newInstance(seriesId: String) = SeriesFragment().apply {
            this.seriesId = seriesId
        }
    }

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentSeriesBinding::bind)

    private val prefs: AppPreferences by inject()

    private var seriesId by FragmentArgumentDelegate<String>()

    private var mediaCallback: MediaCallback? = null

    private val adapter by lazy {
        SeriesAdapter(prefs.getCoordinateFormat(), prefs.getAccuracyThresholdMeters()) { photo ->
            mediaCallback?.editPhoto(photo.path)
        }
    }

    private var current: ApproachSeries? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mediaCallback = context as? MediaCallback
    }

    override fun onDetach() {
        mediaCallback = null
        super.onDetach()
    }

    override fun observeState(state: SeriesContract.State?) {
        when (state) {
            is SeriesContract.State.Content -> render(state.series)
            is SeriesContract.State.Empty -> renderEmpty()
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvFrames.adapter = adapter
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        // Back to the camera with this series still open, so the next shot joins it.
        binding.bMore.setOnClickListener {
            seriesId?.let { id -> mediaCallback?.addToSeries(id) }
        }
        binding.bShare.setOnClickListener {
            current?.let { series -> mediaCallback?.openShare(series.frames.map { it.path }) }
        }
        seriesId?.let { viewModel.sendEvent(SeriesContract.Event.Load(it)) }
    }

    override fun onResume() {
        super.onResume()
        // A frame may have been added from the camera, or deleted from the gallery, since this
        // screen was last on top.
        viewModel.sendEvent(SeriesContract.Event.Reload)
    }

    private fun render(series: ApproachSeries) {
        current = series
        binding.tvEmpty.isVisible = false
        binding.rvFrames.isVisible = true
        binding.bShare.isEnabled = true
        binding.tvSubtitle.text = getString(
            R.string.series_subtitle,
            resources.getQuantityString(R.plurals.series_frames_count, series.size, series.size)
        )
        adapter.bestPath = series.bestFix?.path
        adapter.submitList(series.frames)
        binding.tvExplainer.text = explainerFor(series)
    }

    /** Says which frame the coordinate comes from, by its number in the list and its accuracy. */
    private fun explainerFor(series: ApproachSeries): String {
        val best = series.bestFix
        val accuracy = series.accuracyMeters
        val index = series.frames.indexOfFirst { it.path == best?.path }
        return if (best == null || accuracy == null || index < 0) {
            getString(R.string.series_explainer_no_fix)
        } else {
            getString(R.string.series_explainer, index + 1, Math.round(accuracy))
        }
    }

    private fun renderEmpty() {
        current = null
        adapter.submitList(emptyList())
        binding.tvEmpty.isVisible = true
        binding.rvFrames.isVisible = false
        binding.tvExplainer.text = ""
        binding.tvSubtitle.text = ""
        binding.bShare.isEnabled = false
    }
}
