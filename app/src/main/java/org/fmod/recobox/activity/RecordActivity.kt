package org.fmod.recobox.activity

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.SimpleAdapter
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_record.*
import kotlinx.android.synthetic.main.drawer_header.*
import kotlinx.android.synthetic.main.popup_change_name.view.*
import org.fmod.recobox.R
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.manager.ActivityManager
import org.fmod.recobox.manager.BackEndManager
import org.fmod.recobox.manager.DatabaseManager
import org.fmod.recobox.services.AudioService
import org.fmod.recobox.util.AudioUtil
import org.fmod.recobox.util.Util
import org.fmod.recobox.util.Util.Companion.dp2px
import java.text.SimpleDateFormat
import java.util.*

class RecordActivity : BaseActivity() {

    companion object {
        //未开始录音
        private const val R_STOP = 0
        //正在录音
        private const val R_RECORDING = 1
        //暂停录音
        private const val R_PAUSE = 2

        private var recordState = R_STOP
        //1s内点击两次返回退出
        private const val TWICE_CLICK_INTERVAL = 1000
    }
    //动画管理
    private lateinit var animatorManager: AnimatorManager

    private lateinit var recordRegion: Rect
    private var isInsideRecordRegion = false
    //时间戳，实现防止误触返回键退出程序
    private var startTime = 0L

    //录音开始时间
    private var recordTime = 0L

    //更新录音界面的Runnable
    private lateinit var recordHandler: Handler
    private lateinit var recordThread: Runnable

    private lateinit var mBinder: AudioService.MyBinder
    private val connection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mBinder = service as AudioService.MyBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    private var isConfirmRename = false

    private lateinit var editNicknamePopup: PopupWindow
    private lateinit var editFilenamePopup: PopupWindow

    //录音默认放在根目录
    private lateinit var rootFolder: MyFolder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        ActivityManager.finishRecordAll()
        ActivityManager.add(this)
        getSharedPreferences("data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("qq_login",false)
            .apply()

        init()//初始化各种变量

        setUserInfo()
        setListener()
        setDrawerItems()

        bindService(Intent(this,AudioService::class.java),connection, Context.BIND_AUTO_CREATE)

        rootFolder = DatabaseManager.findRootFolder()
        Log.d(TAG,rootFolder.toString())
    }

    private fun init(){
        animatorManager = AnimatorManager()
        isInsideRecordRegion = false
        //确保不会在打开活后立刻点击返回就退出
        startTime = -System.currentTimeMillis()
        recordTime = 0L
        recordHandler = Handler()
        recordThread = Runnable {
            //限制三十分钟 = 30 * 60 * 1000(ms)
            when(recordState){
                R_RECORDING->{
                    time.text = Util.sec2Time(recordTime++)
                    //限制录制时间30m
                    if (recordTime > 3600){
                        recordStop()
                    }
                    recordHandler.postDelayed(recordThread,1000)
                }
                R_PAUSE->{
                    //暂停不重置时间，等待post

                }
                R_STOP->{
                    time.text = Util.sec2Time(0L)
                }
            }
        }
    }

    private fun setUserInfo(){
        if(isLogin) {
            edit_nickname.visibility = View.VISIBLE
            BaseActivity.nickname = intent.getStringExtra("username")
            cloudContent = intent.getDoubleExtra("size", 0.0)
            val avatar = intent.getStringExtra("avatar")
            Glide.with(this).load(avatar).into(this.avatar)
            nickname.text = BaseActivity.nickname
            val text = "云盘可用空间 ${cloudContent}M/100M"
            cloud_content.text = text
        }else{
            edit_nickname.visibility = View.GONE
            Glide.with(this).load(R.drawable.ic_app_icon).into(this.avatar)
            nickname.text = getString(R.string.default_nickname)
            cloud_content.text = getString(R.string.default_cloud_content)
        }
    }

    private fun setDrawerItems(){
        //修改昵称的popupWindow
        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(this).inflate(R.layout.popup_change_name,null,false)
        editNicknamePopup = PopupWindow(view,dp2px(280f),dp2px(112f),true)
        editNicknamePopup.animationStyle = R.style.PopupAnimation
        editNicknamePopup.elevation = 30f
        /*editNicknamePopup.setOnDismissListener {
            popupShadow(false)
        }*/
        //view.findViewById<TextView>(R.id.hint1).text = "新昵称"
        view.hint1.text = "新昵称"
        view.cancel_change_name.setOnClickListener {
            editNicknamePopup.dismiss()
        }
        view.confirm_change_name.setOnClickListener {
            editNicknamePopup.dismiss()
            if(checkNetworkState()){
                //修改昵称
                nickname.text = view.new_folder_name.text
                BackEndManager.uploadNickname(nickname.text.toString())
            }else{
                showToast("当前网络不可用，修改失败")
            }
            //TODO 修改昵称
        }
        //drawerItem数据源
        val itemList = ArrayList<HashMap<String, Any>>()
        var tempItem: HashMap<String,Any>
        for(i in 1..4){
            tempItem = HashMap()
            tempItem["icon"] = resources.getIdentifier("ic_item$i","drawable",packageName)
            tempItem["text"] = resources.getString(resources.getIdentifier("item$i","string",packageName))
            itemList.add(tempItem)
        }
        val adapter = SimpleAdapter(
            this,
            itemList,
            R.layout.drawer_item,
            arrayOf("icon","text"),
            intArrayOf(R.id.drawer_item_icon, R.id.drawer_item_tv)
        )
        drawer_list.adapter = adapter
        drawer_list.setOnItemClickListener { _, _, position, _ ->
            when(position){
                0->{
                    //我的云盘
                    if(isLogin && checkNetworkState()){
                        startActivity(CloudActivity::class.java)
                    }else if(!isLogin){
                        showToast("未登录")
                    }else{
                        showToast("当前网络不可用，请检查网络设置")
                    }
                }
                1->{
                    //联系团队
                }
                2->{
                    //更多设置
                    startActivity(MoreSettingsActivity::class.java)
                }
                3->{
                    //帮助反馈
                }
            }
        }
    }

    private fun setListener() {
        edit_nickname.setOnClickListener {
            if(isLogin){
                editNicknamePopup.showAtLocation(window.decorView,Gravity.CENTER,0,0)
            }else{
                showToast("您还未登录")
            }
        }


        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(this).inflate(R.layout.popup_change_name,null,false)
        editFilenamePopup = PopupWindow(view,dp2px(280f),dp2px(112f),true)
        editFilenamePopup.animationStyle = R.style.PopupAnimation
        editFilenamePopup.elevation = 30f

        editFilenamePopup.setOnDismissListener {
            val name = if(isConfirmRename){
                view.new_folder_name.text.toString()
            }else{
                val format = SimpleDateFormat("yyyyMMdd_HHmmss",Locale.SIMPLIFIED_CHINESE)
                "新录音${format.format(Date())}"
            }
            showToast("\"$name\"保存成功")
            AudioUtil.createAudioFile(false,name)
            saveFile(name)
        }
        /*editNicknamePopup.setOnDismissListener {
            popupShadow(false)
        }*/
        //view.findViewById<TextView>(R.id.hint1).text = "新昵称"
        view.hint1.text = "重命名"
        view.cancel_change_name.setOnClickListener {
            isConfirmRename = false
            editFilenamePopup.dismiss()
        }
        view.confirm_change_name.setOnClickListener {
            isConfirmRename = true
            editFilenamePopup.dismiss()
        }


        if(!isLogin){
            avatar.setOnClickListener {
                ActivityManager.add(this)
                startActivity(QQLogin::class.java)
            }
        }

        setting_btn.setOnClickListener{
            drawer.post {
                drawer.openDrawer(Gravity.START)
            }
        }

        list_btn.setOnClickListener{
            startActivity(RecordListActivity::class.java)
        }

        record_drop.setOnClickListener {
            recordTime = 0
            recordStop()
            AudioUtil.createAudioFile(true)
        }

        record_finish.setOnClickListener {
            recordStop()
            if(isRename){
                //录音完成后重命名
                editFilenamePopup.showAtLocation(window.decorView,Gravity.CENTER,0,0)
            }else{
                val format = SimpleDateFormat("yyyyMMdd_HHmmss",Locale.SIMPLIFIED_CHINESE)
                val name = "新录音${format.format(Date())}"
                showToast("\"$name\"保存成功")
                AudioUtil.createAudioFile(false,name)
                saveFile(name)
            }
        }

        record_start.setOnTouchListener { v, event ->
            if (!this::recordRegion.isInitialized){
                recordRegion = Rect(record_start.left,record_start.top,record_start.right,record_start.bottom)
            }
            when(event.action){
                MotionEvent.ACTION_DOWN ->{
                    isInsideRecordRegion = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if(isInsideRecordRegion){
                        if(!recordRegion.contains(v.left+event.x.toInt(),v.top+event.y.toInt())) {
                            //手指触摸超出按钮范围
                            animatorManager.animateRecordOutOfRegion()
                            isInsideRecordRegion = false
                        }
                        else if(record_start.scaleX <= animatorManager.maxRecordScale){
                            //此处不封装这短小的代码到animatorManager为了效率
                            record_start.scaleX += animatorManager.recordScaleInterval
                            record_start.scaleY -= animatorManager.recordScaleInterval
                        }
                    }

                }
                MotionEvent.ACTION_UP -> {
                    if(!animatorManager.isRecordAnimationStart() && isInsideRecordRegion){//开始录音
                        when(recordState){
                            R_STOP -> {
                                recordStart()
                            }
                            R_PAUSE -> {
                                recordResume()
                            }
                            R_RECORDING -> {
                                recordPause()
                            }
                        }
                        record_start.post {
                            animatorManager.animateRecord()
                        }
                    }
                }
            }
            true
        }
    }

    private fun saveFile(name: String){
        val bean = MyFile()
        bean.filename = name
        bean.duration = recordTime
        recordTime = 0L
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.SIMPLIFIED_CHINESE)
        bean.date = format.format(Date())
        bean.save()
        rootFolder.fileList.add(bean)
        rootFolder.save()
    }

    //TODO 待封装
    private fun recordStart(){
        recordState = R_RECORDING
        //禁用控件
        setting_btn.isEnabled = false
        list_btn.isEnabled = false
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        animatorManager.animateRecordOption(true)
        record_start.setImageResource(R.drawable.ic_record_stop)
        //服务开始录音
        mBinder.startRecord()
        //更新界面
        recordHandler.postDelayed(recordThread,1000)

        if(isMuteWhenRecord) {
            //来电静音
            setCallMuteOn(true)
        }
    }
    private fun recordResume(){
        recordState = R_RECORDING
        record_start.setImageResource(R.drawable.ic_record_stop)
        //服务继续录音
        if (this::mBinder.isInitialized)
            mBinder.resumeRecord()
        //更新界面
        recordHandler.post(recordThread)
    }
    private fun recordPause(){
        recordState = R_PAUSE
        record_start.setImageResource(R.drawable.ic_record)
        //服务暂停录音
        mBinder.pauseRecord()
    }
    private fun recordStop(){
        recordState = R_STOP
        //启用控件
        setting_btn.isEnabled = true
        list_btn.isEnabled = true
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        mBinder.stopRecord()
        record_start.setImageResource(R.drawable.ic_record)
        record_start.post{
            animatorManager.animateRecord()
            animatorManager.animateRecordOption(false)
        }
        if(isMuteWhenRecord) {
            //来电音量恢复
            setCallMuteOn(false)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MyApp","onPause")
        record_start.scaleX = 1f
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyApp","onDestroy")
        animatorManager.stopRecordAnimation()//防止无法正确回收活动
    }

    override fun onResume() {
        super.onResume()
        animatorManager.animateRecordInLargeScale()

    }

    override fun onBackPressed() {
        when{
            drawer.isDrawerOpen(Gravity.START) -> drawer.closeDrawer(Gravity.START)
            System.currentTimeMillis() - startTime > TWICE_CLICK_INTERVAL -> {
                showToast("再按一次返回键退出")
                startTime = System.currentTimeMillis()
            }
            else -> super.onBackPressed()
        }
    }

    //录音界面动画管理
    inner class AnimatorManager{
        val maxRecordScale = 1.2f
        val recordScaleInterval = 0.008f
        private val maxRecordScaleDuration = 300L

        private val rubberBandAnimatorX = ObjectAnimator.ofFloat(record_start,"scaleX",0f)
        private val rubberBandAnimatorY = ObjectAnimator.ofFloat(record_start,"scaleY",0f)
        //private val fromNothingToOneScaleY = ObjectAnimator.ofFloat(record_drop,"scaleY",0f,1f)
        private val fromNothingToOneScale1 = AnimatorInflater.loadAnimator(this@RecordActivity,R.animator.record_option_animator)
        private val fromNothingToOneScale2 = AnimatorInflater.loadAnimator(this@RecordActivity,R.animator.record_option_animator)

        init {
            fromNothingToOneScale1.setTarget(record_drop)
            fromNothingToOneScale2.setTarget(record_finish)
        }

        fun animateRecordOutOfRegion(){
            if(record_start.scaleX != 1f && !rubberBandAnimatorX.isStarted) {
                val interval = (record_start.scaleX - 1f) / 2
                rubberBandAnimatorX.setFloatValues(
                    record_start.scaleX,
                    1f + 2f * interval,
                    1 - 2f * interval,
                    1 + interval,
                    1 - interval,
                    1f
                )
                rubberBandAnimatorX.duration =
                    (maxRecordScaleDuration * (record_start.scaleX - 1f) / 0.4f).toLong()

                rubberBandAnimatorY.setFloatValues(
                    record_start.scaleY,
                    1f + 2f * interval,
                    1 - 2f * interval,
                    1 + interval,
                    1 - interval,
                    1f
                )
                rubberBandAnimatorY.duration =
                    (maxRecordScaleDuration * (record_start.scaleX - 1f) / 0.4f).toLong()
                rubberBandAnimatorY.start()
                rubberBandAnimatorX.start()
            }
        }

        fun animateRecord(){
            rubberBandAnimatorX.duration = maxRecordScaleDuration
            rubberBandAnimatorX.setFloatValues(record_start.scaleX, 0.8f, 1.2f, 0.9f, 1.1f, 1f)
            rubberBandAnimatorY.duration = maxRecordScaleDuration
            rubberBandAnimatorY.setFloatValues(record_start.scaleX, 1.2f, 0.8f, 1.1f, 0.9f, 1f)
            rubberBandAnimatorY.start()
            rubberBandAnimatorX.start()
        }

        fun animateRecordInLargeScale(){
            rubberBandAnimatorX.duration = 600
            rubberBandAnimatorX.setFloatValues(1f, 0.6f, 1.4f, 0.8f, 1.2f, 1f)
            rubberBandAnimatorY.duration = 600
            rubberBandAnimatorY.setFloatValues(1f, 1.4f, 0.6f, 1.2f, 0.8f, 1f)
            rubberBandAnimatorY.start()
            rubberBandAnimatorX.start()
        }

        fun animateRecordOption(visible: Boolean){
            if(visible) {
                record_drop.visibility = View.VISIBLE
                record_finish.visibility = View.VISIBLE
                record_drop.post {
                    fromNothingToOneScale1.start()
                }
                record_finish.post {
                    fromNothingToOneScale2.start()
                }
            }
            else{
                record_drop.visibility = View.GONE
                record_finish.visibility = View.GONE
            }
        }

        fun stopRecordAnimation() = rubberBandAnimatorX.cancel()

        fun isRecordAnimationStart(): Boolean = rubberBandAnimatorX.isStarted

    }
}
