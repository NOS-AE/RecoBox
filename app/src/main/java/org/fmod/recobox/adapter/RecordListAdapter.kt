package org.fmod.recobox.adapter

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.folder_item.view.*
import kotlinx.android.synthetic.main.record_item.view.*
import org.fmod.recobox.R
import org.fmod.recobox.ViewHolder
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.widget.SlidingMenu

//TODO: 重复使用的bean用暂存变量储存

class RecordListAdapter(
    var mFolderList: ArrayList<MyFolder>,
    var mRecordList: ArrayList<MyFile>): RecyclerView.Adapter<ViewHolder>(){
    var checkState = false
    var moveState = false
    companion object {
        // view types
        const val VIEW_TYPE_EMPTY = 0
        const val VIEW_TYPE_FOLDER = 1
        const val VIEW_TYPE_FILE = 2
        const val VIEW_TYPE_STAR_FOLDER = 3
        const val VIEW_TYPE_BOTTOM = 4
    }

    private var listener: ItemClickListener? = null
    //打开侧滑菜单的item
    private var menuOpenItem: SlidingMenu? = null
    //down并且刚关闭menu
    private var downAndClose = false


    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v: View

        return when(p1){
            VIEW_TYPE_FOLDER -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.folder_item,p0,false)
                setOnCloseTouchListener(v.folder_main_item)
                //监听打开滑动菜单
                (v as SlidingMenu).setOnOpenListener(object : SlidingMenu.OnOpenListener{
                    override fun onOpen(v: View) {
                        menuOpenItem = v as SlidingMenu
                    }
                })
                ViewHolder(v)
            }
            VIEW_TYPE_FILE -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.record_item,p0,false)
                setOnCloseTouchListener(v.record_main_item)
                //监听打开滑动菜单
                (v as SlidingMenu).setOnOpenListener(object : SlidingMenu.OnOpenListener{
                    override fun onOpen(v: View) {
                        menuOpenItem = v as SlidingMenu
                    }
                })
                ViewHolder(v)
            }
            VIEW_TYPE_STAR_FOLDER -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.star_folder_layout,p0,false)
                setOnCloseTouchListener(v)
                v.setOnClickListener {
                    if(!checkState && !moveState) {
                        //进入星标文件夹
                        listener?.clickStarFolder()
                    }
                }
                ViewHolder(v)
            }
            VIEW_TYPE_BOTTOM -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.empty_bottom,p0,false)
                ViewHolder(v)
            }
            else -> {
                //空页面
                v = LayoutInflater.from(p0.context).inflate(R.layout.empty_page,p0,false)
                ViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val v = p0.itemView
        when(p0.itemViewType){
            VIEW_TYPE_FOLDER->{
                (v as SlidingMenu).shut()
                //长按文件夹，进入多选
                v.folder_main_item.setOnLongClickListener {
                    if(!checkState && !moveState) {
                        checkState = true
                        mFolderList[p1].isCheck = true
                        listener?.onCheck(v,true,p1)
                        listener?.onSelect(true)
                        notifyDataSetChanged()
                        true
                    }else
                        false
                }
                //点击文件夹
                v.folder_main_item.setOnClickListener {
                    if(checkState) {
                        mFolderList[p1].isCheck = !mFolderList[p1].isCheck
                        it.folder_check.setImageResource(
                            if(mFolderList[p1].isCheck) {
                                listener?.onSelect(true)
                                R.drawable.ic_check
                            }
                            else {
                                listener?.onUnselect(true)
                                R.drawable.ic_unchecked
                            }
                        )
                    }else {
                        Log.d("MyApp", "click folder when menu open")
                        listener?.clickFolder(mFolderList[p1].id)
                    }
                }
                //点击删除文件夹
                v.slide_delete.setOnClickListener {
                    Log.d("MyApp","remove $p1")
                    menuOpenItem = null
                    notifyItemRemoved(p1)
                    notifyItemRangeChanged(p1,itemCount)
                    listener?.clickFolderDelete(mFolderList[p1].id, p1)
                }

                v.folder_name.text = mFolderList[p1].name
                val info = "内含${mFolderList[p1].num}个音频文件"
                v.folder_info.text = info
                //多选下的视图
                if(checkState || moveState){
                    //多选和移动状态都隐藏文件夹的滑动菜单
                    if(v.slide_delete.visibility != View.GONE)
                        v.slide_delete.visibility =  View.GONE
                }else{
                    if(v.slide_delete.visibility != View.VISIBLE)
                        v.slide_delete.visibility =  View.VISIBLE
                }
                if(checkState){
                    if(v.folder_more.visibility != View.GONE)
                        v.folder_more.visibility = View.GONE
                    v.folder_check.run {
                        if(visibility != View.VISIBLE)
                            visibility = View.VISIBLE
                        setImageResource(
                            if(mFolderList[p1].isCheck)
                                R.drawable.ic_check
                            else
                                R.drawable.ic_unchecked
                        )
                    }
                }else{
                    if(v.folder_more.visibility != View.VISIBLE)
                        v.folder_more.visibility = View.VISIBLE
                    if(v.folder_check.visibility != View.GONE)
                        v.folder_check.visibility = View.GONE
                }
            }
            VIEW_TYPE_FILE->{
                (v as SlidingMenu).shut()
                //长按文件进入多选
                v.record_main_item.setOnLongClickListener {
                    if(!checkState) {
                        checkState = true
                        mRecordList[p1 - mFolderList.size].isCheck = true
                        listener?.onCheck(v,false,p1-mFolderList.size)
                        listener?.onSelect(false)
                        notifyDataSetChanged()
                        true
                    }else
                        false
                }
                //点击more
                setOnCloseTouchListener(v.record_more)
                v.record_more.setOnClickListener {
                    if(!checkState) {
                        menuOpenItem = v
                        v.openMenu()
                    }
                }
                //点击文件
                v.record_main_item.setOnClickListener{
                    if(checkState) {
                        mRecordList[p1 - mFolderList.size].isCheck = !mRecordList[p1 - mFolderList.size].isCheck
                        it.record_check.setImageResource(
                            if(mRecordList[p1 - mFolderList.size].isCheck) {
                                listener?.onSelect(false)
                                R.drawable.ic_check
                            }
                            else {
                                listener?.onUnselect(false)
                                R.drawable.ic_unchecked
                            }
                        )
                    }else {
                        val bean = mRecordList[p1 - mFolderList.size]
                        Log.d("MyApp", "file length: ${bean.duration}")
                        listener?.clickItem(bean)
                    }
                }
                //点击星标
                v.record_slide_star.setOnClickListener {
                    listener?.clickStar(v.record_slide_star,mRecordList[p1 - mFolderList.size])
                }

                //点击删除文件
                v.record_slide_delete.setOnClickListener {
                    Log.d("MyApp","remove $p1")
                    menuOpenItem = null
                    notifyItemRemoved(p1)
                    notifyItemRangeChanged(p1,itemCount)
                    listener?.clickRecordDelete(mRecordList[p1 - mFolderList.size])
                }
                //点击分享
                v.record_slide_share.setOnClickListener {
                    listener?.clickShare(mRecordList[p1 - mFolderList.size].filename)
                }

                //点击播放/停止按钮
                setOnCloseTouchListener(v.record_play_stop)
                v.record_play_stop.setOnClickListener {
                    if(!checkState)
                        listener?.clickPlayStop(it,mRecordList[p1 - mFolderList.size].filename)
                }
                v.record_slide_star.setImageResource(
                    if(mRecordList[p1 - mFolderList.size].star)
                        R.drawable.ic_star_fill
                    else
                        R.drawable.ic_star_white
                )

                v.file_name.text = mRecordList[p1 - mFolderList.size].filename
                v.file_info.text = org.fmod.recobox.util.Util.sec2Time(mRecordList[p1 - mFolderList.size].duration)
                //多选下的视图
                if(checkState){
                    if(v.record_slide_container.visibility != View.GONE)
                        v.record_slide_container.visibility =  View.GONE
                    if(v.record_more.visibility != View.GONE)
                        v.record_more.visibility = View.GONE
                    v.record_check.run {
                        if(visibility != View.VISIBLE)
                            visibility = View.VISIBLE
                        setImageResource(
                            if(mRecordList[p1 - mFolderList.size].isCheck)
                                R.drawable.ic_check
                            else
                                R.drawable.ic_unchecked
                        )
                    }
                }else{
                    if(v.record_slide_container.visibility != View.VISIBLE)
                        v.record_slide_container.visibility =  View.VISIBLE
                    if(v.record_more.visibility != View.VISIBLE)
                        v.record_more.visibility = View.VISIBLE
                    if(v.record_check.visibility != View.GONE)
                        v.record_check.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            mRecordList.size + mFolderList.size == 0 ->
                VIEW_TYPE_EMPTY
            position == itemCount-1->
                VIEW_TYPE_BOTTOM
            position >= mFolderList.size ->
                VIEW_TYPE_FILE
            position == 0 && mFolderList.isNotEmpty() && mFolderList[position].parentId == 1L ->
                //星标文件夹
                VIEW_TYPE_STAR_FOLDER
            else ->
                VIEW_TYPE_FOLDER
        }
    }

    override fun getItemCount(): Int {
        return if(mRecordList.isEmpty() && mFolderList.isEmpty()){
            0//1
        }else{
            mRecordList.size + mFolderList.size + 1
        }
    }

    private fun setOnCloseTouchListener(v: View){
        v.setOnTouchListener { _, event ->
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    downAndClose = !closeSlide()
                    downAndClose
                }
                MotionEvent.ACTION_UP->{
                    if(downAndClose){
                        downAndClose = false
                        true
                    }else{
                        false
                    }
                }
                else-> {
                    if(downAndClose){
                    }
                    downAndClose
                }
            }
        }
    }

    fun setOnItemClickListener(listener: ItemClickListener){
        this.listener = listener
    }

    /**
     * @return 是否允许其他操作
     */
    fun closeSlide(): Boolean{
        val res = menuOpenItem == null
        menuOpenItem?.closeMenu()
        menuOpenItem = null
        return res
    }

    interface ItemClickListener{
        /*Record Item*/
        //点击整个item
        fun clickItem(bean: MyFile)
        //播放/停止
        fun clickPlayStop(v: View,filename: String)
        /*Record Item侧滑菜单*/
        //删除
        fun clickRecordDelete(bean: MyFile)
        //分享
        fun clickShare(filename: String)
        //星标
        fun clickStar(v:View, bean: MyFile)

        /*Folder Item*/
        //点击进入下一层文件夹
        fun clickFolder(parentId: Long)
        //进入星标文件夹
        fun clickStarFolder()
        /*Folder Item侧滑菜单*/
        //删除
        fun clickFolderDelete(parentId: Long, index: Int)

        //item被选择
        fun onCheck(v: View,isFolder: Boolean, index: Int)
        //选择了文件夹或文件
        fun onSelect(isFolder: Boolean)
        //反选了文件或文件夹
        fun onUnselect(isFolder: Boolean)
    }

}