package org.fmod.recobox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import org.fmod.recobox.R
import org.fmod.recobox.util.Util

//侧滑菜单控件，不可拓展，用于侧滑菜单实现和内部显示状态变化
class SlidingMenu(context: Context, attrs: AttributeSet): HorizontalScrollView(context,attrs){
    private var mMenuWidth: Int
    private val openWidth: Int
    private val closeWidth: Int
    private var mScreenWidth = 0
    private var once = true
    private var isOpen = false

    private var isFolder = false
    set(value) {
        field = value
        mMenuWidth = Util.dp2px(if(isFolder) w else w*3)//根据ui提供的数据
    }

    companion object {
        private var isCheck = false
        private const val w = 56f
    }

    init {
        //获取属性值
        val a = context.obtainStyledAttributes(attrs, R.styleable.SlidingMenu)
        try {
            isFolder = a.getBoolean(R.styleable.SlidingMenu_folder,false)
        }finally {
            a.recycle()
        }

        mScreenWidth = Resources.getSystem().displayMetrics.widthPixels
        //由于侧滑菜单的dp大小不变，此处将侧滑菜单宽度写死
        mMenuWidth = Util.dp2px(if(isFolder) w else w*3)//根据ui提供的数据
        openWidth = mMenuWidth / 3
        closeWidth = openWidth * 2
        overScrollMode = View.OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false

        setOnTouch()
    }

    //外界调用，关闭侧滑菜单
    fun closeMenu(){
        close()
    }

    //关闭菜单
    private fun close(){
        smoothScrollTo(0,0)
        isOpen = false
    }

    //打开菜单
    private fun open(){
        smoothScrollTo(mMenuWidth,0)
        isOpen = true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouch(){
        val onTouchListener = OnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_UP -> {
                    //关闭和打开侧滑菜单
                    if(!isCheck) {
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
            wrapper.getChildAt(0).layoutParams.width = mScreenWidth//RecyclerView Item宽度
            //长按item事件
            wrapper.getChildAt(0).setOnLongClickListener {
                if(!isCheck && !isOpen) {
                    Toast.makeText(context, "LongPress", Toast.LENGTH_SHORT).show()
                    isCheck = true
                }
                true
            }
            once = false
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setOnTouchListener(l: OnTouchListener?) {

        //this.setOnTouchListener(onTouchListener)
        super.setOnTouchListener(l)
    }

   /* @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        when(ev?.action){
            MotionEvent.ACTION_UP -> {
                if(Math.abs(scrollX) > mMenuWidth / 2){
                    this.smoothScrollTo(mMenuWidth, 0)
                }else{
                    this.smoothScrollTo(0,0)
                }
                return true
            }
        }
        //不拦截其它事件
        return false
    }*/
}