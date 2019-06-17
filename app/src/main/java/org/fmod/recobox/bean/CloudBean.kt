package org.fmod.recobox.bean

import com.google.gson.annotations.SerializedName

//登录时获取的JsonBean
class CloudBean{
    //用于已登录账号的二次操作
    lateinit var token: String
    //音频列表
    lateinit var data: ArrayList<MyFile>
    //云盘剩余可用空间(m)
    @SerializedName("size")
    var cloudContent = 0.0

    override fun toString(): String {
        return "token:$token cloudContent:$cloudContent"
    }
}