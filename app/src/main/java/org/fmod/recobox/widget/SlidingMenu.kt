package org.fmod.recobox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import org.fmod.recobox.R

//侧滑菜单控件，不可拓展，用于侧滑菜单实现和内部显示状态变化
class SlidingMenu(context: Context, attrs: AttributeSet): HorizontalScrollView(context,attrs){
    private val openWidth: Int
    private val closeWidth: Int
    private val mMenuWidth: Int
    private var once = true

    private var isOpen = false

    private var isFolder = false

    private var listener: OnOpenListener? = null

    companion object {
        private var screenWidth = 0
        private const val folderWidth = 56f
        var checkState = false
        init {
            screenWidth = Resources.getSystem().displayMetrics.widthPixels
        }
    }

    init {
        //获取属性值
        val a = context.obtainStyledAttributes(attrs, R.styleable.SlidingMenu)
        try {
            isFolder = a.getBoolean(R.styleable.SlidingMenu_folder,false)
        }finally {
            a.recycle()
        }
        mMenuWidth = org.fmod.recobox.util.Util.dp2px(if(isFolder) folderWidth else folderWidth * 3)
        openWidth =  mMenuWidth / 3
        closeWidth = openWidth * 2
        overScrollMode = View.OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
        setOnTouch()
    }

    fun setOnOpenListener(listener: OnOpenListener){
        this.listener = listener
    }

    //外界调用，关闭侧滑菜单
    fun closeMenu(){
        if(isOpen) {
            close()
        }
    }

    //外界调用，打开侧滑菜单
    fun openMenu(){
        if(!isOpen){
            open()
        }
    }

    //关闭菜单
    private fun close(){
        smoothScrollTo(0, 0)
        isOpen = false
    }

    //不带动画的关闭菜单
    fun shut(){
        scrollTo(0,0)
        isOpen = false
    }

    //打开菜单
    private fun open(){
        smoothScrollTo(mMenuWidth,0)
        isOpen = true
        listener?.onOpen(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouch(){
        val onTouchListener = OnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_UP -> {
                    //关闭和打开侧滑菜单
                    if(!checkState) {
                        if (isOpen && v.scrollX < closeWidth ||
                            !isOpen && v.scrollX < openWidth
                        ) {
                            close()
                            true
                        } else if (!isOpen && v.scrollX > openWidth ||
                            isOpen && v.scrollX > closeWidth
                        ) {
                            open()
                            true
                        } else {
                            false
                        }
                    }
                    else
                        false
                }
                else -> {
                    false
                }
            }
        }
        this.setOnTouchListener(onTouchListener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(once){
            //此处限制线性布局作为Item菜单的容器
            val wrapper = getChildAt(0) as LinearLayout
            //此处限制item宽度为屏幕大小
            wrapper.getChildAt(0).layoutParams.width = screenWidth//RecyclerView Item宽度
            once = false
        }
        /*checkSwitchLayoutParams()
        if(checkOnce){

        }*/
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setOnTouchListener(l: OnTouchListener?) {

        //this.setOnTouchListener(onTouchListener)
        super.setOnTouchListener(l)
    }

    override fun onDraw(canvas: Canvas?) {
        //checkSwitchLayoutParams()
        super.onDraw(canvas)
    }

    /*private fun checkSwitchLayoutParams(){
        val param: ViewGroup.LayoutParams
        if(checkOnce){
            param = layoutParams
            param.width = screenWidth
            layoutParams = param
            checkOnce = false
        }
        else if(exitCheckOnce){
            param = layoutParams
            param.width = screenWidth + if(isFolder) folderWidth else fileWidth
            layoutParams = param
            exitCheckOnce = false
        }
    }*/

    interface OnOpenListener{
        fun onOpen(v: View)
    }
}