package org.fmod.racobox.decor

import android.content.res.Resources
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

//px
class SpaceDecoration(private val space: Int): RecyclerView.ItemDecoration(){
    companion object {
        val originSapce = -10 * Resources.getSystem().displayMetrics.density.toInt()
    }
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        //因item本身canvas描绘问题，导致显示水平方向本来就有space
        //在此将space设为负值，并封装成传入的参数为初始space
        outRect.right = space + originSapce
        outRect.top = 2
        outRect.bottom = 2
    }
}