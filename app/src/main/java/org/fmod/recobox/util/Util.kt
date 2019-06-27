package org.fmod.recobox.util

import android.content.res.Resources

class Util{
    companion object {
        fun dp2px(dp: Float): Int{
            return (dp * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
        }

        //sec < 60 * 60
        fun sec2Time(sec: Long): String{
            val hour = String.format("%02d",sec/3600)
            val minute = String.format("%02d",sec%3600/60)
            val second = String.format("%02d",sec%60)
            return "$hour:$minute:$second"
        }

        fun sec2TimeNoHour(sec: Long): String{
            val minute = String.format("%02d",sec%3600/60)
            val second = String.format("%02d",sec%60)
            return "$minute:$second"
        }
    }
}