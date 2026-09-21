package com.gps.zazor.ui.outings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gps.zazor.R
import com.gps.zazor.data.models.Outing
import com.gps.zazor.databinding.FragmentMapBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.MediaCallback
import com.gps.zazor.ui.outings.di.injectMapViewModel
import com.gps.zazor.utils.Formats
import com.gps.zazor.utils.export.TrackFormat
import com.gps.zazor.utils.export.shareTrack
import com.gps.zazor.utils.extensions.loadImage
import com.gps.zazor.utils.time.PhotoClock
import com.gps.zazor.utils.viewBinding.viewBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One outing, drawn full screen: the track, its numbered points, and what the chosen point holds.
 *
 * Deliberately not a map SDK - see [com.gps.zazor.views.RouteView] for why. Whoever wants streets
 * under the track sends it out as GPX or KML and opens it wherever they like.
 */
class OutingMapFragment : BaseFragment<OutingsContract.State, OutingsContract.Event>(
    R.layout.fragment_map
) {

    companion object {

        private const val ARG_DAY = "day"

        /** The corners the design rounds the point's thumbnails to. */
        private const val MARKER_RADIUS_DP = 12
        private const val POINT_RADIUS_DP = 16

        /** @param epochDay the outing's date, as days since the epoch. */
        fun newInstance(epochDay: Long) = OutingMapFragment().apply {
            arguments = Bundle().apply { putLong(ARG_DAY, epochDay) }
        }
    }

    override val viewModel by injectMapViewModel()

    private val binding by viewBinding(FragmentMapBinding::bind)

    private var current: Outing? = null

    override fun observeState(state: OutingsContract.State?) {
        when (state) {
            is OutingsContract.State.Content -> render(state)
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getLong(ARG_DAY)?.takeIf { it != 0L }?.let {
            viewModel.sendEvent(OutingsContract.Event.SelectDate(LocalDate.ofEpochDay(it)))
        }
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.tvDate.setOnClickListener(::showDayMenu)
        binding.ivRecenter.setOnClickListener { fitRoute() }
        binding.bOpenPhoto.setOnClickListener { openSelectedPhoto() }
        binding.bSendTrack.setOnClickListener(::showExportMenu)
        binding.vRoute.hidesSelected = true
        binding.vRoute.onPointSelected = {
            renderPoint()
            placeMarker()
        }
        binding.vRoute.onProjected = ::placeMarker
        keepTrackClearOfTheChrome()
        observeEffects()
    }

    override fun onResume() {
        super.onResume()
        viewModel.sendEvent(OutingsContract.Event.Reload)
    }

    /**
     * The stats float over the top of the drawing and the sheet over its foot, so the track is
     * fitted into what is left rather than under them. Measured, because both grow with the text.
     */
    private fun keepTrackClearOfTheChrome() {
        binding.clSheet.post {
            if (view == null) return@post
            binding.vRoute.topInset = binding.llStats.bottom.toFloat()
            binding.vRoute.bottomInset = binding.clSheet.height.toFloat()
        }
    }

    private fun render(state: OutingsContract.State.Content) {
        current = state.selected
        val outing = state.selected
        binding.tvEmpty.isVisible = outing == null
        binding.llStats.isVisible = outing != null
        binding.clSheet.isVisible = outing != null
        if (outing == null) {
            binding.vRoute.setPhotos(emptyList())
            binding.llMarker.isVisible = false
            return
        }

        binding.tvDate.text = outing.date.label()
        binding.statDistance.tvStatValue.text =
            Formats.distance(requireContext(), outing.distanceMeters)
        binding.statDistance.tvStatLabel.setText(R.string.map_stat_route)
        binding.statPoints.tvStatValue.text = outing.pointCount.toString()
        binding.statPoints.tvStatLabel.setText(R.string.map_stat_points)
        binding.statDuration.tvStatValue.text =
            Formats.durationShort(requireContext(), outing.durationSeconds)
        binding.statDuration.tvStatLabel.setText(R.string.map_stat_moving)

        binding.vRoute.setPhotos(outing.photos)
        renderPoint()
        keepTrackClearOfTheChrome()
    }

    /**
     * The chosen point, in the words a person would use about it: what is on the frame, when it
     * was taken, whether it is a panorama and which way it looked, and how tight the fix was.
     */
    private fun renderPoint() {
        val index = binding.vRoute.selectedIndex
        val photo = binding.vRoute.photoAt(index)
        val total = current?.pointCount ?: 0
        if (photo == null) {
            binding.tvPointTitle.setText(R.string.point_none)
            binding.tvPointMeta.text = ""
            binding.tvCounter.isVisible = false
            binding.ivPoint.setImageDrawable(null)
            binding.bOpenPhoto.isEnabled = false
            return
        }
        binding.bOpenPhoto.isEnabled = true
        binding.tvCounter.isVisible = true
        binding.tvCounter.text = getString(R.string.map_point_counter, index + 1, total)
        binding.tvPointTitle.text = photo.name.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.point_title, index + 1, it) }
            ?: getString(R.string.point_title_plain, index + 1)
        binding.tvPointMeta.text = listOfNotNull(
            PhotoClock.formatTime(photo.date),
            photo.bearingDegrees
                ?.takeIf { photo.isWide }
                ?.let { getString(R.string.point_meta_panorama, Math.round(it) % 360) },
            photo.accuracyMeters?.let { getString(R.string.point_meta_accuracy, Math.round(it)) }
        ).joinToString(" · ")
        binding.ivPoint.loadImage(photo.path, circle = false, cornerRadiusDp = POINT_RADIUS_DP)
    }

    /** Pins the photo callout over the point it belongs to, tip down on the spot itself. */
    private fun placeMarker() {
        val photo = binding.vRoute.photoAt(binding.vRoute.selectedIndex)
        val at = binding.vRoute.pointPosition(binding.vRoute.selectedIndex)
        if (photo == null || at == null) {
            binding.llMarker.isVisible = false
            return
        }
        binding.ivMarker.loadImage(photo.path, circle = false, cornerRadiusDp = MARKER_RADIUS_DP)
        binding.llMarker.isVisible = true
        // Measured, then placed: the callout has no size until it has been laid out once, and the
        // point may have moved again by then.
        binding.llMarker.post {
            if (view == null) return@post
            val now = binding.vRoute.pointPosition(binding.vRoute.selectedIndex) ?: return@post
            val marker = binding.llMarker
            val edge = resources.getDimensionPixelSize(R.dimen.ds_space_4)
            // A point near an edge would push the callout off the screen; it stays in view.
            marker.translationX = (binding.vRoute.x + now.x - marker.width / 2F)
                .coerceIn(
                    edge.toFloat(),
                    (binding.root.width - marker.width - edge).toFloat().coerceAtLeast(0F)
                )
            marker.translationY = (binding.vRoute.y + now.y - marker.height)
                .coerceAtLeast(binding.llStats.bottom.toFloat())
        }
    }

    /** Puts the whole track back in view, on the last point of the day. */
    private fun fitRoute() {
        val outing = current ?: return
        binding.vRoute.setPhotos(outing.photos)
        renderPoint()
        placeMarker()
    }

    private fun openSelectedPhoto() {
        val photo = binding.vRoute.photoAt(binding.vRoute.selectedIndex) ?: return
        (activity as? MediaCallback)?.editPhoto(photo.path)
    }

    /** The other days, without going back to the list for them. */
    private fun showDayMenu(anchor: View) {
        val outings = (viewModel.uiState.value as? OutingsContract.State.Content)
            ?.outings.orEmpty()
        if (outings.isEmpty()) return
        PopupMenu(requireContext(), anchor).apply {
            outings.forEachIndexed { index, outing ->
                menu.add(0, index, index, outing.date.label())
            }
            setOnMenuItemClickListener { item ->
                viewModel.sendEvent(
                    OutingsContract.Event.SelectOuting(outings[item.itemId])
                )
                true
            }
        }.show()
    }

    private fun showExportMenu(anchor: View) {
        val outing = current ?: return
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, 0, 0, R.string.export_gpx)
            menu.add(0, 1, 1, R.string.export_kml)
            // Whoever wants real streets under the point gets their own map app, not one here.
            menu.add(0, 2, 2, R.string.route_open_in_maps)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    2 -> openSelectedPointInMaps()
                    else -> {
                        val format =
                            if (item.itemId == 0) TrackFormat.GPX else TrackFormat.KML
                        viewModel.sendEvent(OutingsContract.Event.ExportOuting(outing, format))
                    }
                }
                true
            }
        }.show()
    }

    /** Hands the chosen point to whatever map app is installed, rather than embedding one. */
    private fun openSelectedPointInMaps() {
        val photo = binding.vRoute.photoAt(binding.vRoute.selectedIndex) ?: return
        val lat = photo.lat ?: return
        val lng = photo.lng ?: return
        val label = Uri.encode(
            photo.address?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
        )
        val uri = Uri.parse(
            String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng, label)
        )
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.route_no_maps_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is OutingsContract.Effect.Exported ->
                            shareTrack(effect.file, effect.format)
                        is OutingsContract.Effect.ExportFailed ->
                            Toast.makeText(
                                requireContext(), R.string.export_failed, Toast.LENGTH_SHORT
                            ).show()
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun LocalDate.label(): String = when (this) {
        LocalDate.now() -> getString(R.string.outing_today)
        LocalDate.now().minusDays(1) -> getString(R.string.outing_yesterday)
        else -> format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    }
}
