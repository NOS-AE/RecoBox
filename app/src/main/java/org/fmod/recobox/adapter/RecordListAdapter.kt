package org.fmod.recobox.adapter

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.folder_item.view.*
import kotlinx.android.synthetic.main.record_item.view.*
import okhttp3.internal.Util
import org.fmod.recobox.R
import org.fmod.recobox.ViewHolder
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.widget.SlidingMenu

class RecordListAdapter(
    private var mFolderList: ArrayList<MyFolder>,
    private var mRecordList: ArrayList<MyFile>): RecyclerView.Adapter<ViewHolder>(){

    companion object {
        // view types
        const val VIEW_TYPE_EMPTY = 0
        const val VIEW_TYPE_FOLDER = 1
        const val VIEW_TYPE_FILE = 2
        const val VIEW_TYPE_STAR_FOLDER = 3
        //更多，file item点击更多时创建
        const val VIEW_TYPE_MORE = 4
    }

    private var listener: ItemClickListener? = null
    //打开侧滑菜单的item
    private var menuOpenItem: SlidingMenu? = null
    //更多view的位置
    private var more = -1


    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v: View

        return when(p1){
            VIEW_TYPE_FOLDER -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.folder_item,p0,false)
                v.folder_main_item.setOnClickListener {
                    listener?.clickFolder()
                }
                v.slide_delete.setOnClickListener {
                    listener?.clickFolderDelete()
                }
                ViewHolder(v)
            }
            VIEW_TYPE_FILE -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.record_item,p0,false)
                v.record_play_stop.setOnClickListener {
                    listener?.clickPlayStop()
                }

                ViewHolder(v)
            }
            VIEW_TYPE_STAR_FOLDER -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.star_folder_layout,p0,false)
                v.setOnClickListener {
                    //进入星标文件夹
                    listener?.clickStarFolder()
                }
                ViewHolder(v)
            }
            else -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.empty_page,p0,false)
                ViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val v = p0.itemView
        when(p0.itemViewType){
            VIEW_TYPE_FOLDER->{
                v.folder_name.text = mFolderList[p1].name
                val info = "内含${mFolderList[p1].num}个音频文件"
                v.folder_info.text = info
            }
            VIEW_TYPE_FILE->{
                v.record_more.setOnClickListener {
                    /*if(closeMoreAndSlide()){
                        //TODO 添加更多item
                        more = p1 + 1
                        notifyItemInserted(more)

                    }*/
                    listener?.clickMore(mRecordList[p1 - mFolderList.size].filename)
                }
                v.record_main_item.setOnClickListener{
                    val bean = mRecordList[p1 - mFolderList.size]
                    Log.d("MyApp","file length: ${bean.duration}")
                    listener?.clickItem(bean.filename,bean.duration,bean.description)
                }
                v.file_name.text = mRecordList[p1 - mFolderList.size].filename
                v.file_info.text = org.fmod.recobox.util.Util.sec2Time(mRecordList[p1 - mFolderList.size].duration)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            mRecordList.size + mFolderList.size == 0 ->
                VIEW_TYPE_EMPTY
            position == more ->
                VIEW_TYPE_MORE
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
        return if(mRecordList.size + mFolderList.size == 0){
            1
        }else{
            mRecordList.size +
                    mFolderList.size +
                    if(more == -1) 0 else 1
        }

    }

    fun setOnItemClickListener(listener: ItemClickListener){
        this.listener = listener
    }

    /**
     * @return 是否允许其他操作
     */
    private fun closeMoreAndSlide(): Boolean{
        return when{
            more != -1->{
                notifyItemRemoved(more)
                more = -1
                false
            }
            menuOpenItem != null -> {
                menuOpenItem?.smoothScrollTo(0,0)
                false
            }
            else -> true
        }
    }

    interface ItemClickListener{
        /*Record Item*/
        //点击整个item
        fun clickItem(filename: String, length: Long, description: String)
        //播放/停止
        fun clickPlayStop()
        //更多
        fun clickMore(filename: String)
        /*Record Item侧滑菜单*/
        //删除
        fun clickRecordDelete()
        //剪切
        fun clickClip()
        //分享
        fun clickShare()

        /*Folder Item*/
        //点击进入下一层文件夹
        fun clickFolder()
        //进入星标文件夹
        fun clickStarFolder()
        /*Folder Item侧滑菜单*/
        //删除
        fun clickFolderDelete()
    }

}