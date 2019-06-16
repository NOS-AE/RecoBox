package org.fmod.racobox.activity

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_welcome.*
import org.fmod.racobox.R
import org.fmod.racobox.adapter.WelcomePagerAdapter
import org.fmod.racobox.manager.ActivityManager
//TODO 修复快速点击导致重复进入activity

class WelcomeActivity : BaseActivity() {

    private lateinit var adapter: WelcomePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        ActivityManager.finishWelcomeAll()

        adapter = WelcomePagerAdapter(this)
        adapter.setOnStartToUseClickListener(object: WelcomePagerAdapter.OnStartToUseClickListener{
            override fun onClick() {
                ActivityManager.add(this@WelcomeActivity)
                startActivity(QQLogin::class.java)
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
            }
        })
        welcome_vp.adapter = adapter

    }


    override fun onDestroy() {
        super.onDestroy()
        //停止无限循环的动画，防止内存泄漏
        if(this::adapter.isInitialized)
            adapter.stopStartAnimation()
    }

}
