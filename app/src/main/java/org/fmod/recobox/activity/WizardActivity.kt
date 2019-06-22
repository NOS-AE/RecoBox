package org.fmod.recobox.activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.WindowManager
import org.fmod.recobox.R
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.manager.ActivityManager
import org.fmod.recobox.manager.BackEndManager
import org.fmod.recobox.util.FileUtil
import org.litepal.LitePal
import org.litepal.extension.delete

//如为已登录，且有网络，则发起登录请求，如超时，则提示且不登录
class WizardActivity : BaseActivity() {

    private val hoverTime = 800L
    //Sp data
    private var firstUseApp = false
    private var qqLogin = true
    private lateinit var account:String
    private var network = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wizard)

        ActivityManager.add(this)

        FileUtil.deleteTempMp3()
        getDataFromSp()
        askPermissions()

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        //后端登录回调
        BackEndManager.setOnLoginCallback(object :BackEndManager.UserInfoCallback{
            override fun onComplete(avatar: String, nickname: String, size: Double) {
                val intent = Intent(this@WizardActivity,RecordActivity::class.java)
                intent.putExtra("username",nickname)
                intent.putExtra("avatar",avatar)
                intent.putExtra("size",size)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
            }
        })

        //设置音量控制统一为MUSIC，因为AudioTrack无法动态设置Stream type
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    private fun getDataFromSp(){
        getSharedPreferences("data", Context.MODE_PRIVATE).apply {
            firstUseApp = getBoolean("first_use_app", true)
            qqLogin = getBoolean("qq_login",true)
            account = getString("account","") as String
            isLogin = getBoolean("is_login",false)
            isSpeaker = !getBoolean("is_earpiece",false)
            isRename = getBoolean("is_rename",false)
            isMuteWhenRecord = getBoolean("is_mute",false)
        }

    }

    private fun logic(){
        network = checkNetworkState()
        //检查网络
        if(!network){
            showToast("当前网络不可用，请检查你的网络设置")
        }
        if(firstUseApp){
            //初始化Litepal，创建星标文件夹
            initDataBase()
            //启动欢迎界面
            Handler().postDelayed({
                startActivity(WelcomeActivity::class.java)
            },hoverTime)
        }
        else{
            if(qqLogin){
                //应启动qq登录界面
                Handler().postDelayed({
                    startActivity(QQLogin::class.java)
                },hoverTime)
            }else{
                //应启动录音界面
                //是否已登录
                if(isLogin && network) {
                    Handler().postDelayed({
                        BackEndManager.login(account)
                    }, hoverTime)
                }else{
                    if(!network)
                        isLogin = false
                    Handler().postDelayed({
                        startActivity(RecordActivity::class.java)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }, hoverTime)
                }
            }
        }
    }

    private fun initDataBase(){
        //初始化litepal
        LitePal.initialize(application)
        //创建根目录
        rootFolder = MyFolder()
        rootFolder.run {
            name = "root"
            num = 1
            parentId = -1
        }
        //创建星标文件夹
        val starFolder = MyFolder()
        starFolder.run {
            name = "星标文件"
            num = 0
            parentId = 1
        }

        //根目录下的测试文件
        val testFile = MyFile()
        testFile.run {
            filename = "测试文件"
            description = "没有"
            length = 5.8
            duration = 203
            date = "2018/8/3"
        }
        rootFolder.fileList.add(testFile)
        rootFolder.save()//父目录比子目录和子文件夹先创建
        testFile.save()
        starFolder.save()
        LitePal.delete<MyFile>(testFile.id)//删除测试文件
    }

    private fun askPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),1)
        }else{
            logic()
        }
    }

    private fun finishAndToast(){
        showToast("请开启权限以开启所有功能")
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            1->{
                if(grantResults.isNotEmpty()){
                    //开启权限后，才不为第一次使用
                    grantResults.forEach {
                        if(it != PackageManager.PERMISSION_GRANTED) {
                            finishAndToast()
                            return
                        }
                    }
                    getSharedPreferences("data", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("first_use_app",false)
                        .apply()
                    logic()
                }else{
                    finishAndToast()
                }
            }
        }
    }

    override fun onBackPressed() {
        //Wizard禁止退出...
    }
}
