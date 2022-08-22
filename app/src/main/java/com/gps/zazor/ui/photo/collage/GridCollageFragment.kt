package com.gps.zazor.ui.photo.collage

import android.widget.ImageView
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentGridCollageBinding
import com.gps.zazor.ui.photo.collage.base.CollageFragment
import com.gps.zazor.utils.viewBinding.viewBinding
import io.fotoapparat.view.CameraView

class GridCollageFragment : CollageFragment(R.layout.fragment_grid_collage) {

    private val binding by viewBinding(FragmentGridCollageBinding::bind)

    override val previewViews: List<ImageView> by lazy {
        listOf(binding.ivFirst, binding.ivSecond, binding.ivThird, binding.ivFour)
    }

    override val captureViews by lazy {
        listOf(binding.ivMakeFirst, binding.ivMakeSecond, binding.ivMakeThird, binding.ivMakeFour)
    }

    override val collagePreview by lazy {
        binding.clCollagePreview
    }
}