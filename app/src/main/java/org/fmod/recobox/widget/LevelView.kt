package com.example.racobox.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.TextView
import org.fmod.racobox.R

/*
* 固定一系列属性
* */

class LevelView(context: Context, attrs: AttributeSet): TextView(context,attrs){
    private var mRect = Rect()
    private var mPath = android.graphics.Path()
    private var once = true
    var mTriLeft = true
    var mTriLast = true
    var mText = ""
        set(value) {
            field = value
            text = mText
        }
    private var mTextSize = 0f
    private var mRectWidth = 0f
    private val mPaint = Paint()

    companion object {
        const val grey = 0xFFE0E0E0.toInt()
        const val white = 0xFFFAFAFA.toInt()

        val sRectPadding = 10 * Resources.getSystem().displayMetrics.density
        val sTriWidth = 10 * Resources.getSystem().displayMetrics.density
        val sHeight = 32 * Resources.getSystem().displayMetrics.density
    }

    init {
        //获取属性值
        val a = context.obtainStyledAttributes(attrs,R.styleable.LevelView)
        try {
            mTextSize = a.getLayoutDimension(R.styleable.LevelView_android_textSize,0).toFloat()
        }finally {
            a.recycle()
        }
        //画笔固定属性
        mPaint.style = Paint.Style.FILL
        mPaint.isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //确定文字矩形宽度
        mPaint.textSize = mTextSize
        mPaint.getTextBounds(mText, 0, mText.length, mRect)
        mRectWidth = mRect.width().toFloat()
        val topWidth = mRectWidth + sRectPadding * 2 + sTriWidth
        layoutParams.width = (topWidth + sTriWidth).toInt()

        mPath.reset()
        mPath.lineTo(topWidth, 0f)
        mPath.lineTo(topWidth + sTriWidth, sHeight / 2)
        mPath.lineTo(topWidth, sHeight)
        mPath.lineTo(0f, sHeight)
        if (mTriLeft) {
            mPath.lineTo(sTriWidth, sHeight / 2)
        } else
            mPath.lineTo(0f, 0f)
        once = false

        //setPadding(0,0,(sRectPadding + sTriWidth).toInt(), 0)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        //画背景
        //确定文字矩形宽度
        /*mPaint.textSize = mTextSize
        mPaint.getTextBounds(text.toString(), 0, text.length, mRect)
        mRectWidth = mRect.width().toFloat()
        val topWidth = mRectWidth + sRectPadding * 2 + sTriWidth
        layoutParams.width = (topWidth + sTriWidth).toInt()

        mPath.reset()
        mPath.lineTo(topWidth, 0f)
        mPath.lineTo(topWidth + sTriWidth, sHeight / 2)
        mPath.lineTo(topWidth, sHeight)
        mPath.lineTo(0f, sHeight)
        if (mTriLeft) {
            mPath.lineTo(sTriWidth, sHeight / 2)
        } else
            mPath.lineTo(0f, 0f)
        once = false*/
        mPaint.color = if(mTriLast) grey else white
        canvas?.drawPath(mPath, mPaint)
        super.onDraw(canvas)
    }

}