package org.fmod.recobox.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tencent.connect.UserInfo
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import kotlinx.android.synthetic.main.activity_qqlogin.*
import org.fmod.recobox.R
import org.fmod.recobox.manager.ActivityManager
import org.fmod.recobox.manager.BackEndManager
import org.json.JSONException
import org.json.JSONObject

class QQLogin : BaseActivity() {

    companion object {
        private const val appId = "1109100996"
        lateinit var mTencent: Tencent
        private lateinit var mListener: BaseUiListener
        private lateinit var mUserInfo: UserInfo
        private var once = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qqlogin)

        ActivityManager.finishQQloginAll()
        ActivityManager.add(this)

        setListener()
        if(once) {
            //只创建一个腾讯实例
            mTencent = Tencent.createInstance(appId, applicationContext)
            once = false
        }

        if(mTencent.isSessionValid){
            //启动此界面时，必为退出登录状态
            mTencent.logout(this)
            getSharedPreferences("data", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_login",false)
                .apply()
        }
        mListener = BaseUiListener()

        //后端登录回调
        BackEndManager.setOnLoginCallback(object :BackEndManager.UserInfoCallback{
            override fun onComplete(avatar: String, nickname: String, size: Double) {
                val intent = Intent(this@QQLogin,RecordActivity::class.java)
                intent.putExtra("username",nickname)
                intent.putExtra("avatar",avatar)
                intent.putExtra("size",size)
                isLogin = true
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
            }

            override fun onTimeOut(timeOut: Boolean) {
                runOnUiThread {
                    if(timeOut)
                        showToast("网络连接超时")
                    else
                        showToast("网络连接错误")
                }
                mTencent.logout(this@QQLogin)
            }
        })
    }

    private fun setListener(){
        login.setOnClickListener {
            if(!mTencent.isQQInstalled(this)){
                showToast("未安装QQ")
                return@setOnClickListener
            }
            if(!mTencent.isSessionValid){
                mTencent.login(this,"all", mListener)
            }
        }

        no_login.setOnClickListener {
            getSharedPreferences("data",Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_login",false)
                .apply()
            isLogin = false
            startActivity(RecordActivity::class.java)
            overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
        }
    }

    private fun initOpenIdAndToken(any: Any?){
        val json = any as JSONObject
        try {
            val openId = json.getString("openid")
            val accessToken = json.getString("access_token")
            val expires = json.getString("expires_in")

            mTencent.openId = openId
            mTencent.setAccessToken(accessToken, expires)
        }catch (e: JSONException){
            logcat(e.toString())
        }
    }

    private fun getUserInfo(){
        mUserInfo = UserInfo(this, mTencent.qqToken)
        mUserInfo.getUserInfo(object : IUiListener{
            override fun onCancel() {
                logcat("getUserInfo onCancel")
            }

            override fun onComplete(p0: Any?) {
                //完成获取用户信息
                logcat("getUserInfo on Complete")
                //防止点击“暂不登录”
                no_login.isEnabled = false
                //进入录音界面
                val json = p0 as JSONObject
                try {
                    val name = json.getString("nickname")
                    val avatar = json.getString("figureurl_2")
                    //存账号
                    getSharedPreferences("data", Context.MODE_PRIVATE)
                        .edit()
                        .putString("account", mTencent.accessToken)
                        .putBoolean("is_login",true)
                        .apply()
                    //后端登录
                    BackEndManager.login(mTencent.accessToken,avatar,name)
                }catch (e: JSONException){
                    logcat(e.toString())
                }
            }

            override fun onError(p0: UiError?) {
                isLogin = false
                mTencent.logout(this@QQLogin)
                showToast("QQ登录失败，请检查网络")
                logcat("getUserInfo onError")
            }
        })
    }

    private inner class BaseUiListener: IUiListener{
        override fun onCancel() {
            logcat("QQLogin onCancel")
        }

        override fun onComplete(p0: Any?) {
            logcat("QQLogin onComplete")
            initOpenIdAndToken(p0)
            getUserInfo()
        }

        override fun onError(p0: UiError?) {
            isLogin = false
            mTencent.logout(this@QQLogin)
            showToast("QQ登录失败，请检查网络")
            logcat("QQLogin onError")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Tencent.onActivityResultData(requestCode,resultCode,data,mListener)
    }

}
