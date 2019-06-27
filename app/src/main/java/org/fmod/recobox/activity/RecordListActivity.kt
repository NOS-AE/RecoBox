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
import kotlinx.android.synthetic.main.popup_delete.view.*
import org.fmod.recobox.R
import org.fmod.recobox.adapter.RecordListAdapter
import org.fmod.recobox.bean.CloudBean
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.manager.DatabaseManager
import org.fmod.recobox.util.AudioUtil
import org.fmod.recobox.util.FileUtil
import org.fmod.recobox.util.Util.Companion.dp2px
import org.fmod.recobox.widget.SlidingMenu
import kotlin.collections.ArrayList

class RecordListActivity : BaseActivity() {
    private lateinit var popupManager: PopupWindowManager

    //lateinit var levelAdapter: LevelListAdapter
    private var folderList = ArrayList<MyFolder>()
    private var recordList = ArrayList<MyFile>()
    private lateinit var moveFolderList: ArrayList<MyFolder>
    private lateinit var moveRecordList: ArrayList<MyFile>
    private lateinit var beanToChange: MyFile

    //上层文件夹id
    private var parentId = -1L//根目录1，星标文件2
    //当前目录
    private lateinit var currentFolder: MyFolder
    private lateinit var listAdapter: RecordListAdapter

    private var isPlaying = false
    private var checkState = false
    private var moveState = false
    private var checkAll = false
    private var downAndClose = false
    private lateinit var playingView: ImageView
    private var folderCheck = 0
    private var recordCheck = 0

    private var editEnable = false
    private var moveEnable = false
    private var deleteEnable = false
    private var shareEnable = false


    private lateinit var enterCheckAnimator: ObjectAnimator
    private lateinit var enterCheckAnimator2: ObjectAnimator
    private lateinit var enterCheckAnimator3: ObjectAnimator
    //private lateinit var enterCheckAnimator4: ObjectAnimator
    private lateinit var exitCheckAnimator: ObjectAnimator
    private lateinit var exitCheckAnimator2: ObjectAnimator
    private lateinit var exitCheckAnimator3: ObjectAnimator
    //private lateinit var exitCheckAnimator4: ObjectAnimator

   // private lateinit var mSearchView: android.support.v7.widget.SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_list)

        popupManager = PopupWindowManager()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setCheckAnimator()
        setListener()
        setRecyclerView()

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
                resetRecyclerView()
                //setRecyclerView()
                if(!moveState) {
                    //恢复新建文件夹按钮
                    fab.show()
                }
                parentId = folder.parentId
            }

            override fun onFindStarFiles(starFileList: ArrayList<MyFile>) {
                //加载星标文件完成，显示列表
                recordList = starFileList
                folderList = ArrayList()
                logcat("starFile:" + starFileList.size.toString())
                resetRecyclerView()
                //setRecyclerView()
                //星标文件夹不许有子目录，隐藏新建文件夹按钮
                fab.hide()
                parentId = 1L
            }
        })

        DatabaseManager.findCurrentList(1,true)
    }


    private fun enterCheckAnimate(){
        record_list_title.text = getString(R.string.check_title)
        enterCheckAnimator.start()
        enterCheckAnimator2.start()
        enterCheckAnimator3.start()
        //enterCheckAnimator4.start()
        fab.hide()

        check_option_layout.visibility = View.VISIBLE
    }

    private fun exitCheckAnimate(){
        record_list_title.text = getString(R.string.list_title)
        exitCheckAnimator.start()
        exitCheckAnimator2.start()
        exitCheckAnimator3.start()
        //exitCheckAnimator4.start()
        fab.show()

        check_option_layout.visibility = View.GONE
        val title = "我的录音"
        record_list_title.text = title
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
                check_all.text = if(folderList.size + recordList.size == 1) "取消全选" else "全选"
                check_all.visibility = View.VISIBLE
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
        /*enterCheckAnimator4 = ObjectAnimator.ofInt(
            check_all,
            "textColor",
            getColor(R.color.colorTheme),
            getColor(R.color.colorFont))
        enterCheckAnimator4.duration = 400L
        enterCheckAnimator4.setEvaluator(ArgbEvaluator())*/

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
                check_all.visibility = View.GONE
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
        /*exitCheckAnimator4 = ObjectAnimator.ofInt(
            check_all,
            "textColor",
            getColor(R.color.colorFont),
            getColor(R.color.colorTheme)
        )
        exitCheckAnimator4.duration = 200L
        exitCheckAnimator4.setEvaluator(ArgbEvaluator())*/
    }

    private fun resetRecyclerView(){
        empty_list_bg.visibility = if(folderList.isEmpty() && recordList.isEmpty()){
            list_rv.visibility = View.GONE
            View.VISIBLE
        }else{
            listAdapter.mFolderList = folderList
            listAdapter.mRecordList = recordList
            listAdapter.notifyDataSetChanged()
            logcat("findlist and notify")
            list_rv.visibility = View.VISIBLE
            View.GONE
        }
    }

    private fun setRecyclerView(){
        empty_list_bg.visibility = View.VISIBLE
        val verticalLayoutManager = LinearLayoutManager(this)
        listAdapter = RecordListAdapter(folderList,recordList)
        listAdapter.setOnItemClickListener(object : RecordListAdapter.ItemClickListener{
            override fun clickStar(v: View,bean: MyFile) {
                bean.star = !bean.star
                (v as ImageView).setImageResource(
                    if(bean.star)
                        R.drawable.ic_star_fill
                    else
                        R.drawable.ic_star_white
                )
                bean.save()
            }
            override fun clickFolder(parentId: Long) {
                stopMusic()
                var state = true
                if(moveState){
                    for(i in moveFolderList){
                        if(i.isCheck && i.id == parentId){
                            showToast("不允许进入将要被移动的文件夹中")
                            state = false
                            break
                        }
                    }
                }
                if(state)
                    DatabaseManager.findCurrentList(parentId,!moveState)
            }

            override fun clickFolderDelete(parentId: Long, index: Int) {
                folderList.removeAt(index)
                if(folderList.isEmpty() && recordList.isEmpty())
                    empty_list_bg.visibility = View.VISIBLE
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

            override fun onCheck(v: View,isFolder: Boolean, index: Int) {
                if(!checkState){
                    //进入checkState
                    checkState = true
                    //清空check状态
                    folderCheck = 0
                    recordCheck = 0
                    for(i in folderList){
                        i.isCheck = false
                    }
                    for(i in recordList){
                        i.isCheck = false
                    }
                    if(isFolder){
                        folderList[index].isCheck = true
                    }else{
                        recordList[index].isCheck = true
                    }
                    Log.d(TAG,"Enter checkState")
                    SlidingMenu.checkState = true
                    enterCheckAnimate()
                }
            }

            override fun onSelect(isFolder: Boolean) {
                when{
                    isFolder -> {
                        folderCheck++
                    }
                    !isFolder -> {
                        recordCheck++
                    }
                }
                updateCheckOption()
            }

            override fun onUnselect(isFolder: Boolean) {
                when{
                    isFolder -> {
                        folderCheck--
                    }
                    !isFolder -> {
                        recordCheck--
                    }
                }
                updateCheckOption()
            }

            override fun clickRecordDelete(bean: MyFile) {
                recordList.remove(bean)
                if(folderList.isEmpty() && recordList.isEmpty())
                    empty_list_bg.visibility = View.VISIBLE
                DatabaseManager.deleteFile(bean)
            }

            override fun clickShare(filename: String) {
                FileUtil.shareFile(filename,this@RecordListActivity)
                popupManager.showLoadingPopup()
            }

            override fun clickStarFolder() {
                stopMusic()
                DatabaseManager.findStarFile()
            }

            override fun clickItem(bean: MyFile) {
                stopMusic()
                val intent = Intent(this@RecordListActivity,RecordPlayActivity::class.java)
                beanToChange = bean
                intent.putExtra("filename",bean.filename)
                intent.putExtra("length",bean.duration)
                intent.putExtra("description",bean.description)
                logcat("RecordList ${bean.length}")
                startActivity(intent)
            }
        })
        list_rv.layoutManager = verticalLayoutManager
        list_rv.adapter = listAdapter
    }

    private fun updateCheckOption(){
        val title = "已选择${folderCheck + recordCheck}个"
        record_list_title.text = title

        if(folderCheck==folderList.size && recordCheck==recordList.size) {
            checkAll = true
            check_all.text = "取消全选"
        }
        else  {
            checkAll = false
            check_all.text = "全选"
        }

        editEnable = folderCheck==1 && recordCheck==0 ||
                folderCheck==0 && recordCheck==1
        moveEnable = if(currentFolder.id == 1L && parentId == 1L)
            false
        else
            folderCheck != 0 || recordCheck != 0
        deleteEnable = folderCheck!=0 || recordCheck!=0
        shareEnable = folderCheck==0 && recordCheck!=0

        check_option_edit.setCompoundDrawablesWithIntrinsicBounds(
            0,if(editEnable) R.drawable.ic_edit_square_enable
            else R.drawable.ic_edit_square_unenable,0,0
        )

        check_option_move.setCompoundDrawablesWithIntrinsicBounds(
            0,if(moveEnable) R.drawable.ic_move_to_enable
            else R.drawable.ic_move_to_unenable,0,0
        )

        check_option_delete.setCompoundDrawablesWithIntrinsicBounds(
            0,if(deleteEnable) R.drawable.ic_delete_enable
            else R.drawable.ic_delete_unenable,0,0
        )

        check_option_share.setCompoundDrawablesWithIntrinsicBounds(
            0,if(shareEnable) R.drawable.ic_share_enable
            else R.drawable.ic_share_unenable,0,0
        )

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

    private fun exitCheckState(){
        checkState = false
        SlidingMenu.checkState = false
        listAdapter.checkState = false
        listAdapter.notifyDataSetChanged()
    }

    private fun setListener(){
        check_all.setOnClickListener {
            if(checkAll){
                for(i in folderList)
                    i.isCheck = false
                for(i in recordList)
                    i.isCheck = false
                (it as TextView).text = "全选"
                recordCheck = 0
                folderCheck = 0
            }else{
                for(i in folderList)
                    i.isCheck = true
                for(i in recordList)
                    i.isCheck = true
                (it as TextView).text = "取消全选"
                recordCheck = recordList.size
                folderCheck = folderList.size
            }
            checkAll = !checkAll
            listAdapter.notifyDataSetChanged()
            updateCheckOption()
        }

        check_option_edit.setOnClickListener {
            if(editEnable){
                popupManager.showChangeNamePopup()
            }
        }

        check_option_move.setOnClickListener {
            if(moveEnable){
                moveState = true
                checkState = false
                listAdapter.checkState = false
                listAdapter.moveState = true
                //保存当前多选状态下的列表
                moveFolderList = folderList
                moveRecordList = recordList
                //界面更新
                check_all.visibility = View.GONE
                move_confirm.visibility = View.VISIBLE
                check_option_layout.visibility = View.GONE
                record_list_title.text = getString(R.string.move_title)
                listAdapter.mRecordList = ArrayList()//移动时不显示文件
                listAdapter.notifyDataSetChanged()
            }
        }

        check_option_delete.setOnClickListener {
            if(deleteEnable){
                DatabaseManager.deleteFiles(recordList)
                recordList.removeAll {
                    it.isCheck
                }
                folderList.removeAll {
                    if(it.isCheck){
                        DatabaseManager.deleteFolder(it.id)
                    }
                    it.isCheck
                }
                if(recordList.isEmpty() && folderList.isEmpty())
                    empty_list_bg.visibility = View.VISIBLE
                listAdapter.notifyDataSetChanged()
                exitCheckState()
                exitCheckAnimate()
            }

        }

        check_option_share.setOnClickListener {
            if(shareEnable){
                val shareList = ArrayList<String>()
                for(i in recordList){
                    if(i.isCheck) {
                        shareList.add(i.filename)
                    }
                }
                FileUtil.shareMultiFile(shareList,this)
                popupManager.showLoadingPopup()
                exitCheckState()
                exitCheckAnimate()
            }

        }

        speaker_earpiece_switch.setImageResource(
            if(!isSpeaker){
                R.drawable.ic_earpiece
            }else{
                R.drawable.ic_speaker
            }
        )
        speaker_earpiece_switch.setOnClickListener {
            setSpeakerOn()

        }

        return_record_list.setOnClickListener{
            when{
                checkState -> {
                    onBackPressed()
                }
                moveState -> {
                    exitMoveState()
                }
                else->{
                    stopMusic()
                    finish()
                }
            }
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
            popupManager.showNewFolderPopup()
        }

        move_confirm.setOnClickListener {
            DatabaseManager.move(moveRecordList,moveFolderList,currentFolder)
            //更新视图
            exitCheckAnimate()
            exitMoveState()
        }
    }

    private fun exitMoveState(){
        moveState = false
        listAdapter.moveState = false
        move_confirm.visibility = View.GONE
        fab.show()
        exitCheckAnimate()
        DatabaseManager.findCurrentList(currentFolder.id,true)
    }

    private fun stopMusic(){
        AudioUtil.stopMusic()
        isPlaying = false
        if(this::playingView.isInitialized)
            playingView.setImageResource(R.drawable.ic_play)
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
                exitCheckState()
                exitCheckAnimate()
            }
            parentId == -1L -> {
                if(moveState){
                    exitMoveState()
                }else {
                    stopMusic()
                    super.onBackPressed()
                }
            }
            else -> DatabaseManager.findCurrentList(parentId,!moveState)
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
            view.hint1.text = "新名字"
            popupChangeName = PopupWindow(view,dp2px(280f),dp2px(112f),true)
            popupChangeName.animationStyle = R.style.PopupAnimation
            popupChangeName.elevation = 30f
            popupChangeName.setOnDismissListener {
                popupShadow(false)
            }
            view.cancel_change_name.setOnClickListener {
                popupChangeName.dismiss()
            }
            view.confirm_change_name.setOnClickListener {
                popupChangeName.dismiss()
                changeName(popupChangeName.contentView.new_folder_name.text.toString())
                exitCheckState()
                exitCheckAnimate()
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
            view.cancel_delete.setOnClickListener {
                popupDelete.dismiss()
            }
            view.confirm_delete.setOnClickListener {
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
            view.cancel_change_name.setOnClickListener {
                popupNewFolder.dismiss()
            }
            view.confirm_change_name.setOnClickListener {
                newFolder(popupNewFolder.contentView.new_folder_name.text.toString())
                popupNewFolder.dismiss()
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
        private fun changeName(name: String){
            if(folderCheck == 1){
                for(i in folderList){
                    if(i.isCheck){
                        i.name = name
                        i.save()
                        break
                    }
                }
            }else{
                for(i in recordList){
                    if(i.isCheck){
                        FileUtil.renameFile(i.filename,name)
                        i.filename = name
                        i.save()
                        break
                    }
                }
            }
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
            resetRecyclerView()
            //listAdapter.notifyDataSetChanged()
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

        fun showChangeNamePopup(){
            popupChangeName.showAtLocation(window.decorView, Gravity.CENTER,0,0)
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

    override fun onResume() {
        super.onResume()
        if(description != null){
            beanToChange.description = (description as String)
            beanToChange.save()
        }
    }

}
