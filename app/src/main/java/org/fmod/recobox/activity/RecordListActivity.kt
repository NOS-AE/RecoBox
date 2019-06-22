package org.fmod.recobox.activity

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.activity_record_list.*
import kotlinx.android.synthetic.main.popup_change_name.view.*
import org.fmod.recobox.R
import org.fmod.recobox.adapter.RecordListAdapter
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.manager.DatabaseManager
import org.fmod.recobox.util.AudioUtil
import org.fmod.recobox.util.FileUtil
import org.fmod.recobox.util.Util.Companion.dp2px
import java.util.*

class RecordListActivity : BaseActivity() {
    private lateinit var popupManager: PopupWindowManager

    //lateinit var levelAdapter: LevelListAdapter
    lateinit var folderList: ArrayList<MyFolder>
    lateinit var recordList: ArrayList<MyFile>

    //上层文件夹id
    private var parentId = -1L//根目录1，星标文件2
    //当前目录
    private lateinit var currentFolder: MyFolder
    private lateinit var listAdapter: RecordListAdapter

    private var isPlaying = false
    private var checkState = false
    private var downAndClose = false
    private lateinit var playingView: ImageView

    private lateinit var enterCheckAnimator: ObjectAnimator
    private lateinit var enterCheckAnimator2: ObjectAnimator
    private lateinit var enterCheckAnimator3: ObjectAnimator
    private lateinit var exitCheckAnimator: ObjectAnimator
    private lateinit var exitCheckAnimator2: ObjectAnimator
    private lateinit var exitCheckAnimator3: ObjectAnimator

   // private lateinit var mSearchView: android.support.v7.widget.SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_list)

        popupManager = PopupWindowManager()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setCheckAnimator()
        setListener()

        AudioUtil.setPlayListener(object : AudioUtil.PlayListener{
            override fun onStop() {
                isPlaying = false
                runOnUiThread {
                    playingView.setImageResource(R.drawable.ic_play)
                }
            }
        })

        FileUtil.setOnEncodeListener(object : FileUtil.EncodeListener{
            override fun onComplete() {
                listAdapter.closeSlide()
                runOnUiThread {
                    popupManager.hideLoadingPopup()
                }
            }
        })

        DatabaseManager.setOnFindCallback(object : DatabaseManager.Companion.FindCallBack{
            override fun onFind(folder: MyFolder, folderList: ArrayList<MyFolder>) {
                //从数据库加载数据完成，显示列表
                currentFolder = folder
                this@RecordListActivity.folderList = folderList
                recordList = folder.fileList
                logcat(folder.toString())
                setRecyclerView()
                //恢复新建文件夹按钮
                fab.show()
                parentId = folder.parentId
            }

            override fun onFindStarFiles(starFileList: ArrayList<MyFile>) {
                //加载星标文件完成，显示列表
                recordList = starFileList
                folderList = ArrayList()
                logcat("starFile:" + starFileList.size.toString())
                setRecyclerView()
                //星标文件夹不许有子目录，隐藏新建文件夹按钮
                fab.hide()
                parentId = 1L
            }
        })

        DatabaseManager.findCurrentList(1)
    }

    private fun enterCheckAnimate(){
        record_list_title.text = getString(R.string.check_title)
        enterCheckAnimator.start()
        enterCheckAnimator2.start()
        enterCheckAnimator3.start()
        fab.hide()
    }

    private fun exitCheckAnimate(){
        record_list_title.text = getString(R.string.list_title)
        exitCheckAnimator.start()
        exitCheckAnimator2.start()
        exitCheckAnimator3.start()
        fab.show()
    }

    private fun setCheckAnimator(){
        //多选进入和退出的颜色动画
        enterCheckAnimator = ObjectAnimator.ofInt(
            toolbar,
            "backgroundColor",
            getColor(R.color.colorTheme),
            getColor(R.color.white_bg))
        enterCheckAnimator.duration = 400L
        enterCheckAnimator.setEvaluator(ArgbEvaluator())
        enterCheckAnimator.addListener(object : Animator.AnimatorListener{
            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                speaker_earpiece_switch.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }
        })

        enterCheckAnimator2 = ObjectAnimator.ofInt(
            return_record_list,
            "colorFilter",
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt())
        enterCheckAnimator2.duration = 400L
        enterCheckAnimator2.setEvaluator(ArgbEvaluator())
        enterCheckAnimator3 = ObjectAnimator.ofInt(
            record_list_title,
            "textColor",
            0xFFFFFFFF.toInt(),
            getColor(R.color.colorFont))
        enterCheckAnimator3.duration = 400L
        enterCheckAnimator3.setEvaluator(ArgbEvaluator())

        exitCheckAnimator =  ObjectAnimator.ofInt(
            toolbar,
            "backgroundColor",
            getColor(R.color.white_bg),
            getColor(R.color.colorTheme))
        exitCheckAnimator.duration = 200L
        exitCheckAnimator.setEvaluator(ArgbEvaluator())
        exitCheckAnimator.addListener(object : Animator.AnimatorListener{
            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {
                speaker_earpiece_switch.visibility = View.VISIBLE
            }
        })
        exitCheckAnimator2 =  ObjectAnimator.ofInt(
            return_record_list,
            "colorFilter",
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt())
        exitCheckAnimator2.duration = 200L
        exitCheckAnimator2.setEvaluator(ArgbEvaluator())
        exitCheckAnimator3 =  ObjectAnimator.ofInt(
            record_list_title,
            "textColor",
            getColor(R.color.colorFont),
            0xFFFFFFFF.toInt())
        exitCheckAnimator3.duration = 200L
        exitCheckAnimator3.setEvaluator(ArgbEvaluator())
    }

    private fun setRecyclerView(){
        val verticalLayoutManager = LinearLayoutManager(this)
        listAdapter = RecordListAdapter(folderList,recordList)
        listAdapter.setOnItemClickListener(object : RecordListAdapter.ItemClickListener{
            override fun clickFolder(parentId: Long) {
                DatabaseManager.findCurrentList(parentId)
            }

            override fun clickFolderDelete(parentId: Long, index: Int) {
                folderList.removeAt(index)
                DatabaseManager.deleteFolder(parentId)
            }

            override fun clickPlayStop(v: View,filename: String) {
                isPlaying = !isPlaying
                playingView = v as ImageView
                if(isPlaying){
                    playingView.setImageResource(R.drawable.ic_stop)
                    AudioUtil.playMusic(filename)
                }else{
                    playingView.setImageResource(R.drawable.ic_play)
                    AudioUtil.stopMusic()
                }
            }

            override fun onCheck(v: View) {
                if(!checkState){
                    //进入checkState
                    checkState = true
                    Log.d(TAG,"RecordListActivity enter check")
                    //return_record_list.setColorFilter(0xFF000000.toInt())
                    enterCheckAnimate()
                }

            }

            override fun clickRecordDelete(bean: MyFile) {
                recordList.remove(bean)
                DatabaseManager.deleteFile(bean)
            }

            override fun clickShare(filename: String) {
                FileUtil.shareFile(filename,this@RecordListActivity)
                popupManager.showLoadingPopup()
            }

            override fun clickStarFolder() {
                DatabaseManager.findStarFile()
            }

            override fun clickItem(filename: String, length: Long, description: String) {
                val intent = Intent(this@RecordListActivity,RecordPlayActivity::class.java)
                intent.putExtra("filename",filename)
                intent.putExtra("length",length)
                intent.putExtra("description",description)
                logcat("RecordList $length")
                startActivity(intent)
            }
        })
        list_rv.layoutManager = verticalLayoutManager
        list_rv.adapter = listAdapter
    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        //在此处设置SearchView
        val searchItem = menu?.findItem(R.id.menu_search)
        mSearchView = searchItem?.actionView as android.support.v7.widget.SearchView
        mSearchView.queryHint = resources.getString(R.string.search_hint)
        mSearchView.setOnQueryTextListener(object: android.support.v7.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextChange(p0: String?): Boolean {
                //
                return true
            }

            override fun onQueryTextSubmit(p0: String?): Boolean {
                mSearchView.onActionViewCollapsed()
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)

    }*/

    private fun setListener(){
        speaker_earpiece_switch.run {
            setOnClickListener {
                setSpeakerOn()
                if(!isSpeaker){
                    setImageResource(R.drawable.ic_earpiece)
                }else{
                    setImageResource(R.drawable.ic_speaker)
                }
            }
        }

        return_record_list.setOnClickListener{
            finish()
        }

        fab.setOnTouchListener { _, event ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    downAndClose = !listAdapter.closeSlide()
                    downAndClose
                }
                MotionEvent.ACTION_UP -> {
                    if(downAndClose) {
                        downAndClose = false
                        true
                    }else{
                        false
                    }
                }
                else -> downAndClose
            }
        }

        fab.setOnClickListener {
            //popupManager.showChangeName("",0)
            //popupManager.showDeletePopup(0)
            //popupManager.showNewPopup()
            //popupManager.showSharePopup(0)
            /*levelAdapter.addLevel("测试Level")
            rv.post {
                rv.smoothScrollToPosition(list.size - 1)
            }*/
            //FileUtil.shareFile("test",this)
            /*val bean = MyFile()
            bean.filename = "test"
            bean.date = "2018/8/8"
            bean.description = "没事"
            bean.duration = 15
            bean.length = 11.2
            BackEndManager.uploadFiles(bean)*/
            //startActivity(RecordPlayActivity::class.java)
            popupManager.showNewFolderPopup()
        }

        move_confirm.setOnClickListener {
            Toast.makeText(this,"Move",Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroy() {
        //释放未关闭的popupWindow
        popupManager.closeAll()
        //删除暂存的MP3

        super.onDestroy()
    }

    override fun onBackPressed() {
        when{
            checkState -> {
                checkState = false
                for(i in folderList){
                    if (i.isCheck)
                        i.isCheck = false
                }
                for(i in recordList){
                    if(i.isCheck)
                        i.isCheck = false
                }
                listAdapter.checkState = false
                exitCheckAnimate()
            }
            parentId == -1L -> super.onBackPressed()
            else -> DatabaseManager.findCurrentList(parentId)
        }

    }



    //管理PopupWindow的类
    inner class PopupWindowManager{
        //更改文件名字
        private val popupChangeName: PopupWindow
        //提示删除录音
        private val popupDelete: PopupWindow
        //新建文件夹
        private val popupNewFolder: PopupWindow
        //加载动画
        private val popupLoading: PopupWindow
        //用于新建选项
        //private val popupNew: PopupWindow

        //private var mBefore = ""
        //private var mId = 0

        //初始化popup
        init {
            //为PopupWindows加载布局，设置监听
            //用于更改文件名字
            @SuppressLint("InflateParams")
            var view = LayoutInflater.from(this@RecordListActivity).inflate(R.layout.popup_change_name,null,false)
            popupChangeName = PopupWindow(view,dp2px(280f),dp2px(112f),true)
            popupChangeName.animationStyle = R.style.PopupAnimation
            popupChangeName.elevation = 30f
            popupChangeName.setOnDismissListener {
                popupShadow(false)
            }
            view.findViewById<TextView>(R.id.cancel_change_name).setOnClickListener {
                popupChangeName.dismiss()
            }
            view.findViewById<TextView>(R.id.confirm_change_name).setOnClickListener {
                popupChangeName.dismiss()
                changeName()
            }
            //用于提示删除录音
            @SuppressLint("InflateParams")
            view = LayoutInflater.from(this@RecordListActivity).inflate(R.layout.popup_delete,null,false)
            popupDelete = PopupWindow(view,dp2px(280f),dp2px(112f),true)
            popupDelete.animationStyle = R.style.PopupAnimation
            popupDelete.elevation = 16f
            popupDelete.setOnDismissListener {
                popupShadow(false)
            }
            view.findViewById<TextView>(R.id.cancel_delete).setOnClickListener {
                popupDelete.dismiss()
            }
            view.findViewById<TextView>(R.id.confirm_delete).setOnClickListener {
                popupDelete.dismiss()
                delete()
            }
            //用于新建文件夹
            @SuppressLint("InflateParams")
            view = LayoutInflater.from(this@RecordListActivity).inflate(R.layout.popup_change_name,null,false)
            popupNewFolder = PopupWindow(view,dp2px(280f),dp2px(112f),true)
            popupNewFolder.animationStyle = R.style.PopupAnimation
            popupNewFolder.elevation = 30f
            popupNewFolder.setOnDismissListener {
                popupShadow(false)
            }
            view.findViewById<TextView>(R.id.cancel_change_name).setOnClickListener {
                popupNewFolder.dismiss()
            }
            view.findViewById<TextView>(R.id.confirm_change_name).setOnClickListener {
                popupNewFolder.dismiss()
                newFolder(view.new_folder_name.text.toString())
            }
            //加载动画
            @SuppressLint("InflateParams")
            view = LayoutInflater.from(this@RecordListActivity).inflate(R.layout.popup_loading,null,false)
            popupLoading = PopupWindow(view,dp2px(112f),dp2px(112f),true)
            popupLoading.animationStyle = R.style.PopupAnimation
            popupLoading.elevation = 30f
            popupLoading.setOnDismissListener {
                popupShadow(false)
            }
            //用于新建选项
            /*@SuppressLint("InflateParams")
            view = LayoutInflater.from(this@RecordListActivity).inflate(R.layout.popup_new,null,false)
            popupNew = PopupWindow(view,dp2px(250f),dp2px(112f),true)
            popupNew.animationStyle = R.style.PopupAnimation
            popupNew.elevation = 16f
            popupNew.setOnDismissListener {
                popupShadow(false)
            }*/
        }

        //使整个窗口加上阴影
        //TODO 改为加上蒙版，而不是改变透明度
        private fun popupShadow(isShow: Boolean){
            val lp = window.attributes
            lp.alpha = if(isShow)
                0.75f
            else
                1f
            window.attributes = lp
        }

        //内部popup操作函数
        //更改item名字
        private fun changeName(){
            //TODO 利用id更改item名字和更新数据库

        }
        //删除item
        private fun delete(){
            //TODO 利用id删除item和更新数据库
        }

        private fun newFolder(name: String){
            val bean = MyFolder()
            bean.name = name
            bean.num = 0
            bean.parentId = currentFolder.id
            folderList.add(bean)
            bean.save()
            listAdapter.notifyDataSetChanged()
        }

        /**
         *外部popup调用显示
         *更改名字popup
         * //@param before 更改前的名字
         * //@param id 被修改item在rv列表中的id
         */
        /*fun showChangeNamePopup(before: String, id: Int){
            mBefore = before
            mId = id
            popupChangeName.showAtLocation(window.decorView,Gravity.CENTER,0,0)
            popupShadow(true)
        }*/

        fun showNewFolderPopup(){
            popupNewFolder.showAtLocation(window.decorView,Gravity.CENTER,0,0)
            popupShadow(true)
        }

        fun showLoadingPopup(){
            popupLoading.showAtLocation(window.decorView, Gravity.CENTER,0,0)
            popupShadow(true)
        }

        fun hideLoadingPopup(){
            popupLoading.dismiss()
        }
        /**
         * 确认删除popup
         * //@param id 被删除item在rv中的id
         * */
        /*fun showDeletePopup(id: Int){
            mId = id
            popupDelete.showAtLocation(window.decorView,Gravity.CENTER,0,0)
            popupShadow(true)
        }
        /**
         * 新建popup
         * */
        fun showNewPopup(){
            /*popupNew.showAtLocation(window.decorView,
                Gravity.CENTER_HORIZONTAL or Gravity.TOP,
                0,dp2px(448f))
            popupShadow(true)*/
        }*/

        //外界onDestroy时关闭popup，防止泄露
        fun closeAll(){
            when{
                popupChangeName.isShowing -> popupChangeName.dismiss()
                popupDelete.isShowing -> popupDelete.dismiss()
                //popupNew.isShowing -> popupNew.dismiss()
            }
        }
    }

}
