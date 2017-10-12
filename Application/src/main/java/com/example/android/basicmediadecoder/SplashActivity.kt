package com.example.android.basicmediadecoder

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import kotlinx.android.synthetic.main.activity_splash.*
import java.io.IOException
import android.content.res.AssetFileDescriptor
import android.view.View
import org.jetbrains.anko.startActivity


class SplashActivity : Activity(), SurfaceHolder.Callback {

    //    private var videoView = null
    private var holder:SurfaceHolder? = null
    private val mediaPlayer:MediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val path = "android.resource://" + packageName + "/" + R.raw.waves

        holder = videoView.holder
        holder?.addCallback(this)

        broadcastButton.setOnClickListener {
            startActivity<MainActivity>()
        }

        receiverButton.setOnClickListener {
            startActivity<MainActivity>()
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        mediaPlayer.setDisplay(holder)
        if(mediaPlayer.isPlaying) return
        // Loop the video
        mediaPlayer.setOnPreparedListener({
            mediaPlayer -> mediaPlayer.isLooping = true
        })
        val path = "android.resource://" + packageName + "/" + R.raw.waves
        try{
            mediaPlayer.setDataSource(this, Uri.parse(path))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
    }

}
