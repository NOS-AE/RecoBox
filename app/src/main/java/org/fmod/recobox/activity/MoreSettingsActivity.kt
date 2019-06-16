package org.fmod.racobox.activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_more_settings.*
import org.fmod.racobox.R
import org.fmod.racobox.manager.ActivityManager

class MoreSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_settings)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorTheme)
        setListener()
        initState()
    }

    private fun initState(){
        phone_mode_switch.isChecked = isSpeaker
        mute_when_ring_switch.isChecked = isMuteWhenRecord
        rename_after_record_switch.isChecked = isRename
    }

    private fun setListener(){
        phone_mode_switch.setOnCheckedChangeListener { _, isChecked ->
            isSpeaker = !isChecked
            getSharedPreferences("data", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_earpiece", isSpeaker)
                .apply()
        }

        mute_when_ring_switch.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if(!manager.isNotificationPolicyAccessGranted){
                    //未开启免打扰权限
                    Snackbar.make(mute_when_ring_switch,"请开启免打扰权限",Snackbar.LENGTH_SHORT)
                        .setAction("去开启"){
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }.show()
                    mute_when_ring_switch.isChecked = false
                    return@setOnCheckedChangeListener
                }
            }
            isMuteWhenRecord = isChecked
            getSharedPreferences("data", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_mute",isChecked)
                .apply()
        }

        rename_after_record_switch.setOnCheckedChangeListener { _, isChecked ->
            isRename = isChecked
            getSharedPreferences("data", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_rename",isChecked)
                .apply()
        }
        if(isLogin) {
            exit_account_layout.setOnClickListener {
                //退出登录
                getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("qq_login", true)
                    .apply()
                ActivityManager.add(this)
                startActivity(QQLogin::class.java)
            }
        }else{
            exit_account_layout.visibility = View.GONE
        }

        return_record.setOnClickListener {
            finish()
        }
    }
}
