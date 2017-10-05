package com.example.android.basicmediadecoder

import android.os.Bundle
import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : Activity() {

//    private var videoView = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val path = "android.resource://" + packageName + "/" + R.raw.waves

        videoView.setOnPreparedListener(MediaPlayer.OnPreparedListener {
            mediaPlayer -> mediaPlayer.isLooping = true
        })

        videoView.setVideoURI(Uri.parse(path))
        videoView.start()
    }

}
