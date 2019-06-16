package org.fmod.racobox.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import org.fmod.racobox.R
import org.fmod.racobox.ViewHolder

class LevelListAdapter(private var mList: ArrayList<String>, private val context:Context): RecyclerView.Adapter<ViewHolder>(){

    /**
     * 添加一层
     * @param name 新层名字
     * */

    val startBg = ContextCompat.getDrawable(context, R.drawable.level)
    val Bg = ContextCompat.getDrawable(context, R.drawable.level_right)
    val endBg = ContextCompat.getDrawable(context,R.drawable.level_end)

    fun addLevel(name: String){
        mList.add(name)
        notifyItemInserted(mList.size)
        notifyItemChanged(mList.size - 2)
    }

    /**
     * 删除最后几层
     * @param num 删除的层数
     * */
    fun removeLevel(num: Int){
        for(i in 1..num)
            mList.removeAt(mList.lastIndex)
        notifyItemRangeRemoved(mList.size - 1,num)
        notifyItemChanged(mList.size - 2)
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        /*val v = (p0.itemView as LevelView)
        /*if(p1 == 0)
            v.mTriLeft = false/=*/
        v.mTriLeft = p1 != 0
        v.mTriLast = (p1 == mList.size - 1)
        v.mText = mList[p1]*/
        val v = (p0.itemView as TextView)
        //v.setBackgroundResource(R.drawable.level)
        //val padding = 10 * Resources.getSystem().displayMetrics.density.toInt()
        /*if(p1 == 0)
            v.mTriLeft = false/=*/
        v.text = mList[p1]
        v.background = when (p1) {
            0->startBg
            mList.size-1 -> endBg
            else -> Bg
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v = LayoutInflater.from(p0.context).inflate(R.layout.level_item,p0,false)
        val holder = ViewHolder(v)
        v.setOnClickListener {
            Toast.makeText(context,"${holder.layoutPosition}",Toast.LENGTH_SHORT).show()
        }
        return holder
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}