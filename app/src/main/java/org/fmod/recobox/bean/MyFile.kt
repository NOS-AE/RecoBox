package org.fmod.racobox.bean

import com.google.gson.annotations.SerializedName
import org.fmod.racobox.util.Util
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport
import java.util.*
import kotlin.properties.Delegates

//云端列表，本地列表

class MyFile: LitePalSupport(){
    @Column(unique = true)
    var id = 0L
    @Column(unique = true)
    var filename = ""
    //文件创建日期和时间（精确到分）
    var date = ""
    //录音时长（s）
    var duration = 0L
    var durationString = "01:50:15"
    //文件大小
    var length = 0.0
    //备注
    var description = ""
    //是否星标
    var star = false

    override fun toString(): String {
        return "filename:$filename date:$date duration:$duration description:$description size:$length"
    }
}