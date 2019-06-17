package org.fmod.recobox.activity

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import org.fmod.recobox.R
import org.fmod.recobox.bean.MyFolder

abstract class BaseActivity: AppCompatActivity(){
    companion object {
        const val TAG = "MyApp"
        //全局活动共用
        //是否已登录
        var isLogin = false
        //录音结束后重命名
        var isRename = false
        //默认是否扬声器
        var isSpeaker = true
        //录音是是否来电静音
        var isMuteWhenRecord = false
        //用户名
        lateinit var nickname: String
        //可用空间
        var cloudContent = 0.0
        //根目录
        lateinit var rootFolder: MyFolder

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BaseActivity", "onCreate")
        //DisplayUtil.setCustomDensity(this, application)
    }


    //跳转活动
    //-1不作为resultCode
    protected fun <T>startActivity(cls: Class<T>,anim: Boolean = true, bundle: Bundle? = null, resultCode: Int = -1){
        val intent = Intent(this,cls)
        if(bundle != null){
            intent.putExtra("bundle",bundle)
        }
        if(resultCode == -1)
            startActivity(intent)
        else
            startActivityForResult(intent, resultCode)
        if(anim){
            //重载动画
            overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
        }
    }

    protected fun showToast(msg: String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }

    protected fun logcat(msg: String){
        Log.d(TAG,msg)
    }

    protected fun checkNetworkState(): Boolean{
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }

    protected fun setSpeakerOn(){
        isSpeaker = !isSpeaker
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.run {
            isSpeakerphoneOn = isSpeaker
            mode = if(isSpeaker) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION
        }
    }

    protected fun setCallMuteOn(on: Boolean){
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.run {
            if(on){
                /*preVol = getStreamVolume(AudioManager.STREAM_RING)
                //来电静音
                adjustStreamVolume(
                    AudioManager.STREAM_RING,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )*/
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }else{
                //恢复音量
                /*setStreamVolume(
                    AudioManager.STREAM_RING,
                    preVol,
                    AudioManager.FLAG_SHOW_UI
                )*/
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            //audioManager.ringerMode
        }
    }

}