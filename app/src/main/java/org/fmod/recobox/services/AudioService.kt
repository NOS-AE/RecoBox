package org.fmod.recobox.services

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import org.fmod.recobox.R
import org.fmod.recobox.activity.RecordActivity
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
        //初始化notification builder
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("record","RecoBox",NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val pIntent = Intent(Intent.ACTION_MAIN)
        pIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pName = intent!!.getStringExtra("package_name")
        val cName = intent.getStringExtra("class_name")
        pIntent.component = ComponentName(
            pName,
            "$pName.$cName"
        )
        Log.d("MyApp","$cName $pName")
        pIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

        val cIntent = Intent(this,RecordActivity::class.java)
        cIntent.action = Intent.ACTION_MAIN
        cIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        cIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY

        val pendingIntent = PendingIntent.getActivity(this,0, cIntent,0)

        notification = NotificationCompat.Builder(this,"record")
            .setContentTitle("RecoBox")
            .setContentText("正在录音")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentIntent(pendingIntent)
            .build()
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"onCreate")
        //startForeground(1,notification)
        //startForeground(1,notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }


    inner class MyBinder: Binder(){
        fun startRecord(){
            Log.d(TAG,"startRecord")
            AudioUtil.startRecord("test")
            notificationManager.notify(1,notification)
        }
        fun pauseRecord(){
            Log.d(TAG,"pauseRecord")
            AudioUtil.pauseRecord()
            notificationManager.cancel(1)
        }
        fun resumeRecord(){
            Log.d(TAG,"resumeRecord")
            AudioUtil.resumeRecord()
            notificationManager.notify(1,notification)
        }
        fun stopRecord(){
            Log.d(TAG,"stopRecord")
            AudioUtil.stopRecord()
            notificationManager.cancel(1)
        }
    }
}