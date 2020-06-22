package com.ktvipin.cameraxsample.ui.media

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.utils.Config.VIDEO_FILE_EXTENSION
import kotlinx.android.synthetic.main.fragment_media_viewer.*

class MediaViewerFragment : Fragment(R.layout.fragment_media_viewer) {

    private val args: MediaViewerFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_back.setOnClickListener { findNavController().popBackStack() }

        val mediaUri = args.mediaUri
        if (mediaUri.path?.endsWith(VIDEO_FILE_EXTENSION, true) == true) {
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