package org.fmod.racobox.bean

import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport

//标记id统一为较早创建的id较小
//数据库id最小为1，自定义id最小为0
class MyFolder: LitePalSupport(){
    companion object {
        //下标为level，值为在在当前level中的id
        @Column(ignore = true)
        var levelIdList = ArrayList<Int>()
    }
    @Column(unique = true)
    val id = 0L
    //上层文件夹id
    var parentId = 0L
    //文件夹名
    var name: String = "新建文件夹"
    //文件夹所在的虚拟目录，0为根目录，每进入一层目录+1
    var num = 0
    //文件夹包含的文件
    var fileList = ArrayList<MyFile>()

    override fun toString(): String {
        return "id:$id name:$name num:$num fileList:${fileList.size}"
    }
}