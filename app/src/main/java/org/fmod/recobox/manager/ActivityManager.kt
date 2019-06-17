package org.fmod.recobox.manager

import android.app.Activity
import org.fmod.recobox.activity.*

class ActivityManager{
    companion object {
        private val activities = ArrayList<Activity>()

        fun add(a: Activity) {
            activities.removeAll {
                a::class == it::class
            }
            activities.add(a)
        }

        //activities移除操作都用迭代器来移除元素
        //避免ConcurrentModificationException
        //参考https://www.cnblogs.com/dolphin0520/p/3933551.html

        fun finishWelcomeAll(){
            val iterator = activities.iterator()
            var i: Activity
            while(iterator.hasNext()){
                i = iterator.next()
                if(i is WizardActivity){
                    i.finish()
                    iterator.remove()
                }
            }
        }

        fun finishQQloginAll(){
            val iterator = activities.iterator()
            var i: Activity
            while(iterator.hasNext()){
                i = iterator.next()
                if(i is WizardActivity ||
                    i is WelcomeActivity ||
                    i is RecordActivity ||
                    i is MoreSettingsActivity
                ){
                    i.finish()
                    iterator.remove()
                }
            }
        }

        fun finishRecordAll(){
            val iterator = activities.iterator()
            var i: Activity
            while(iterator.hasNext()){
                i = iterator.next()
                if(i is WizardActivity ||
                    i is QQLogin ||
                    i is RecordActivity){
                    i.finish()
                    iterator.remove()
                }
            }
        }
    }
}