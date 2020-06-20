package com.ktvipin.cameraxsample.ui.preview

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ktvipin.cameraxsample.R
import kotlinx.android.synthetic.main.fragment_preview.*

class PreviewFragment : Fragment(R.layout.fragment_preview) {

    private val args: PreviewFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_back.setOnClickListener { findNavController().popBackStack() }
        val mediaUri = args.mediaUri
        if (mediaUri?.path?.endsWith(".mp4", true) == true) {
            val mediaController = MediaController(requireContext())
            video_view.setMediaController(mediaController)
            video_view.setVideoURI(mediaUri)
            video_view.seekTo(1)
            video_view.visibility = VISIBLE
            photo_view.visibility = GONE
        } else {
            photo_view.setImageURI(mediaUri)
            photo_view.visibility = VISIBLE
            video_view.visibility = GONE
        }
    }
}