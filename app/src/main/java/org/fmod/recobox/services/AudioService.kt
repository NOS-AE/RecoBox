package org.fmod.recobox.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import org.fmod.recobox.R
import org.fmod.recobox.util.AudioUtil

class AudioService: Service(){

    private val mBinder = MyBinder()

    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification
    companion object {
        private const val TAG = "AudioService"
    }


    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG,"onbind")
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        //初始化notification builder
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("record","RecoBox",NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        notification = NotificationCompat.Builder(this,"record")
            .setContentTitle("RecoBox")
            .setContentText("正在录音")
            .setSmallIcon(R.drawable.ic_app_icon).build()

        Log.d(TAG,"oncreate")
        //startForeground(1,notification)
        //startForeground(1,notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"onstartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class MyBinder: Binder(){
        fun startRecord(){
            Log.d(TAG,"startrecord")
            AudioUtil.startRecord("test")
            notificationManager.notify(1,notification)
        }
        fun pauseRecord(){
            Log.d(TAG,"pauserecord")
            AudioUtil.pauseRecord()
            notificationManager.cancel(1)
        }
        fun resumeRecord(){
            Log.d(TAG,"resumerecord")
            AudioUtil.resumeRecord()
            notificationManager.notify(1,notification)
        }
        fun stopRecord(){
            Log.d(TAG,"stoprecord")
            AudioUtil.stopRecord()
            notificationManager.cancel(1)
        }
    }
}