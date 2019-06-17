package org.fmod.recobox.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.EditText
import android.widget.PopupWindow
import kotlinx.android.synthetic.main.activity_record_play.*
import kotlinx.android.synthetic.main.clip_layout.view.*
import org.fmod.recobox.R
import org.fmod.recobox.util.AudioUtil
import org.fmod.recobox.util.Util
import kotlin.concurrent.thread

class RecordPlayActivity : BaseActivity() {

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler

    private var isPlaying = false

    var time = 0
    var start = false

    private var filename = ""
    private var length = 0

    private lateinit var popupDescription: PopupWindow
    private lateinit var popupClip: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_play)

        runnable = Runnable {
            if(isPlaying) {
                time++
                play_record_time.text = Util.sec2Time(time.toLong())
                play_progress.progress = time
                handler.postDelayed(runnable,1000)
            }else{
                play_progress.progress = 0
                play_record_time.text = Util.sec2Time(0L)

            }
        }

        handler = Handler()

        if(isSpeaker){
            record_play_speaker_earpiece_switch.setImageResource(R.drawable.ic_speaker_black)
        }else{
            record_play_speaker_earpiece_switch.setImageResource(R.drawable.ic_earpiece_black)
        }

        AudioUtil.setPlayListener(object : AudioUtil.PlayListener{
            override fun onStop() {
                runOnUiThread {
                    isPlaying = false
                    play_record.setImageResource(R.drawable.ic_bottom_play)
                }
            }
        })

        filename = intent.getStringExtra("filename")
        length = intent.getLongExtra("length",0L).toInt()
        description.text = intent.getStringExtra("description")
        record_play_title.text = filename
        play_progress.min = 0
        play_progress.max = length
        play_progress.progress = 0
        val str = Util.sec2Time(length.toLong())
        logcat("RecordPlay $length")
        progress_total_time.text = str.substring(str.length - 5)
        setListener()
    }

    private fun setListener(){
        record_play_speaker_earpiece_switch.setOnClickListener {
            setSpeakerOn()
        }

        edit_description.setOnClickListener {
            popupDescription.showAtLocation(window.decorView, Gravity.CENTER,0,0)
        }

        record_play_clip.setOnClickListener {
            AudioUtil.stopMusic()
            popupClip.showAtLocation(window.decorView, Gravity.CENTER,0,0)
        }

        play_record.setOnClickListener {
            if(isPlaying){
                play_record.setImageResource(R.drawable.ic_bottom_play)
                AudioUtil.stopMusic()
            }else{
                time = 0
                handler.postDelayed(runnable,1000)
                play_record.setImageResource(R.drawable.ic_record_stop)
                thread {
                    AudioUtil.playMusic(filename,play_progress.progress.toFloat() / play_progress.max)
                }
            }
            isPlaying = !isPlaying
        }

        play_progress.setOnTouchListener { _, event ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    AudioUtil.stopMusic()
                    play_record.setImageResource(R.drawable.ic_bottom_play)
                }
            }
            false
        }

        @SuppressLint("InflateParams")
        var view = LayoutInflater.from(this).inflate(R.layout.description_layout,null,false)
        (view as EditText).run {
            setSingleLine(false)
            filters = arrayOf(InputFilter.LengthFilter(100))
            //isHorizontalScrollBarEnabled = false
        }
        popupDescription = PopupWindow(view, Util.dp2px(280f), Util.dp2px(300f),true)
        popupDescription.animationStyle = R.style.PopupAnimation
        popupDescription.elevation = 30f
        popupDescription.setOnDismissListener {
            description.text = (popupDescription.contentView as EditText).text.toString()
        }

        @SuppressLint("InflateParams")
        view = LayoutInflater.from(this).inflate(R.layout.clip_layout,null,false)
        //view.clip_range.setValue(0f,length.toFloat())
        view.confirm_play.setOnClickListener {
            val current = view.clip_range.currentRange
            AudioUtil.playMusic(filename,current[0] / length, current[1] / length)
        }
        view.confirm_clip.setOnClickListener {
            popupClip.dismiss()
            AudioUtil
        }
        popupClip = PopupWindow(view, Util.dp2px(280f), Util.dp2px(150f),true)
        popupClip.animationStyle = R.style.PopupAnimation
        popupClip.elevation = 30f


    }


}
