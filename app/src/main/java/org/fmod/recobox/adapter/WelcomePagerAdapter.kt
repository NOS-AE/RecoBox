package org.fmod.recobox.adapter


import android.animation.ObjectAnimator
import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.fmod.recobox.R

class WelcomePagerAdapter(private val mContext: Context): PagerAdapter(){

    //“开始使用”按钮点击接口
    private var mOnStartToUseClickListener: OnStartToUseClickListener? = null
    private lateinit var startAnimation: ObjectAnimator

    //自定义函数
    fun setOnStartToUseClickListener(listener: OnStartToUseClickListener){
        mOnStartToUseClickListener = listener
    }

    //重载函数
    override fun getCount(): Int {
        return 3
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view: View

        //最后一个导航页增加了一个“开始使用”按钮
        if(position == 2){
            view = View.inflate(mContext, R.layout.welcome_vp_with_btn, null)
            val startToUseBtn = view.findViewById<ImageView>(R.id.start_to_use_btn)
            startToUseBtn.setOnClickListener {
                mOnStartToUseClickListener?.onClick()
            }
            startAnimation = ObjectAnimator.ofFloat(startToUseBtn,"alpha",0.9f,0.2f,0.9f)
            startAnimation.duration = 2000L
            startAnimation.repeatCount = ObjectAnimator.INFINITE
            startAnimation.start()

        } else{
            view = View.inflate(mContext,R.layout.welcome_vp, null)
            if (position == 0) {
                view.findViewById<ImageView>(R.id.welcome_vp_iv).setImageResource(R.drawable.wizard1)
            }
            else if(position == 1){
                view.findViewById<ImageView>(R.id.welcome_vp_iv).setImageResource(R.drawable.wizard2)
            }
        }
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(p0: View, p1: Any): Boolean {
        return  p0 == p1
    }

    fun stopStartAnimation(){
        startAnimation.cancel()
    }

    //“开始使用”按钮点击接口
    interface OnStartToUseClickListener{
        fun onClick()
    }
}