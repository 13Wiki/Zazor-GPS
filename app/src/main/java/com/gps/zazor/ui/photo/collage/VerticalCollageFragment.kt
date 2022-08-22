package com.gps.zazor.ui.photo.collage

import android.widget.ImageView
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentVerticalCollageBinding
import com.gps.zazor.ui.photo.collage.base.CollageFragment
import com.gps.zazor.utils.viewBinding.viewBinding
import io.fotoapparat.view.CameraView

class VerticalCollageFragment : CollageFragment(R.layout.fragment_vertical_collage) {

    private val binding by viewBinding(FragmentVerticalCollageBinding::bind)

    override val captureViews: List<ImageView> by lazy {
        listOf(binding.ivMakeFirst, binding.ivMakeSecond)
    }

    override val previewViews: List<ImageView> by lazy {
        listOf(binding.ivFirst, binding.ivFour)
    }

    override val collagePreview by lazy {
        binding.clCollagePreview
    }
}