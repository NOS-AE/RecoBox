package org.fmod.recobox.adapter

import android.content.res.Resources
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.cloud_item_layout.view.*
import kotlinx.android.synthetic.main.cloud_layout.view.*
import org.fmod.recobox.R
import org.fmod.recobox.ViewHolder
import org.fmod.recobox.bean.MyFile

class CloudListAdapter(
    private var mList: ArrayList<MyFile>,
    private var nickname: String,
    private var cloudContent: Double): RecyclerView.Adapter<ViewHolder>(){

    private lateinit var listener: OnItemClickListener

    companion object {
        //云
        const val VIEW_TYPE_TOP = 0
        //item
        const val VIEW_TYPE_EMPTY = 1
        //empty page
        const val VIEW_TYPE_ITEM = 2
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val v = p0.itemView
        if (p0.itemViewType == VIEW_TYPE_ITEM){
            val bean = mList[p1-1]
            v.cloud_filename.text = bean.filename
            val text = "${bean.date} | ${bean.length}M"
            v.cloud_file_info.text = text
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v: View
        return when(p1){
            VIEW_TYPE_TOP -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.cloud_layout, p0,false)
                //云的高度适配(h=32, w=45)
                val param = v.cloud_top.layoutParams
                param.height = (Resources.getSystem().displayMetrics.widthPixels.toFloat() / 45 * 32).toInt()
                v.layoutParams = param
                v.cloud_nickname.text = nickname
                val text = "可用空间 ${cloudContent}M/100M"
                v.cloud_cloud_content.text = text
                ViewHolder(v)
            }
            VIEW_TYPE_ITEM -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.cloud_item_layout,p0,false)
                v.download.setOnClickListener {
                    if(this::listener.isInitialized) {
                        listener.clickDownload(v.download_progress,v.cloud_filename.text.toString())
                        v.download.visibility = View.INVISIBLE
                        v.download_progress.visibility = View.VISIBLE
                    }
                }
                ViewHolder(v)
            }else -> {
                v = LayoutInflater.from(p0.context).inflate(R.layout.empty_page,p0,false)
                ViewHolder(v)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            position == 0 -> VIEW_TYPE_TOP
            mList.isNotEmpty() && position > 0 -> VIEW_TYPE_ITEM
            else -> VIEW_TYPE_EMPTY
        }
    }

    override fun getItemCount(): Int {
        //云+1
        return 1 + if(mList.isNotEmpty()) mList.size else 1
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    interface OnItemClickListener{
        fun clickDownload(progress: View,filename: String)
    }
}