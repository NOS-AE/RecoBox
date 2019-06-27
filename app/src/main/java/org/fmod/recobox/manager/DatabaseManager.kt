package org.fmod.recobox.manager


import android.util.Log
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.fmod.recobox.util.FileUtil
import org.litepal.LitePal
import org.litepal.extension.delete
import kotlin.concurrent.thread

//管理数据库，提供需要展示的数据，简化Activity的操作（数据库初始化位于WelcomeActivity）
//异步即时获取数据
//所有Activity共用
class DatabaseManager{
    companion object {
        //当前目录
        private lateinit var folder: MyFolder
        //当前目录的子目录
        private lateinit var folderList: ArrayList<MyFolder>
        private var mCallBack: FindCallBack? = null

        private var isAnotherFind = false//另一个线程是否完成

        /**
         * 查询当前目录下的列表
         * 被包含文件夹不加载关联表
         * @param parentId 父目录id
         * */
        fun findCurrentList(parentId: Long, findFile: Boolean){
            //开始查询
            // 加载当前目录，获取文件关联表
            Log.d("MyApp",parentId.toString())
            LitePal.findAsync(MyFolder::class.java, parentId,findFile).listen {
                folder = it
                if(isAnotherFind){
                    mCallBack?.onFind(folder, folderList)
                }
                isAnotherFind = !isAnotherFind
            }
            // 加载当前目录下的文件夹（非关联），不获取文件关联表
            LitePal.where("parentId = $parentId")
                //.order("id desc")
                .findAsync(MyFolder::class.java).listen {
                    folderList = it as ArrayList<MyFolder>
                    if(isAnotherFind){
                        mCallBack?.onFind(folder, folderList)
                    }
                    isAnotherFind = !isAnotherFind
                }
        }

        fun findStarFile(){
            LitePal.where("star = 1")
                .findAsync(MyFile::class.java).listen {
                    mCallBack?.onFindStarFiles(it as ArrayList<MyFile>)
                }
        }

        fun findRootFolder(): MyFolder{
            return LitePal.find(MyFolder::class.java,1,true)
        }

        //删除数据库中文件，包括真实文件
        fun deleteFile(bean: MyFile){
            FileUtil.deleteFile(bean.filename)
            LitePal.delete(MyFile::class.java, bean.id)
        }

        fun deleteFiles(list: ArrayList<MyFile>){
            FileUtil.deleteFiles(list)
            for(i in list){
                if(i.isCheck){
                    LitePal.delete(MyFile::class.java, i.id)
                }
            }
        }

        //删除数据库中文件夹，包括record和subfolder，和真实文件
        fun deleteFolder(id: Long){
            thread {
                FileUtil.deleteFiles(LitePal.select("filename")
                    .where("myfolder_id = $id")
                    .find(MyFile::class.java) as ArrayList<MyFile>)
                deleteSubfolder(id)
                LitePal.delete<MyFolder>(id)
            }
        }

        //删除当前目录下的子目录
        /**
         * @param parentId 父目录id
         * */
        private fun deleteSubfolder(parentId: Long){
            //删除parentId下的真实文件
            FileUtil.deleteFiles(LitePal.select("filename")
                .where("myfolder_id = $parentId")
                .find(MyFile::class.java) as ArrayList<MyFile>)
            //寻找parentId下的子目录并删除
            LitePal.where("parentId = $parentId")
                .findAsync(MyFolder::class.java)
                .listen {
                    for(i in it){
                        deleteSubfolder(i.id)
                    }
                    LitePal.delete<MyFolder>(parentId)
                }
        }

        fun move(fileList: ArrayList<MyFile>, folderList: ArrayList<MyFolder>, currentFolder: MyFolder){
            /*if(fileList.isNotEmpty()){
                if(fileList[0].myfolder_id == currentFolder.id) return
                for(i in fileList){
                    if(i.isCheck) {
                        currentFolder.fileList.add(i)
                        i.myfolder_id = currentFolder.id
                    }
                }
                currentFolder.save()
                LitePal.saveAll(fileList)
            }*/
            if(folderList.isNotEmpty()){
                if(folderList[0].parentId == currentFolder.id) return
                for(i in folderList){
                    if(i.isCheck)
                        i.parentId = currentFolder.id
                }
                LitePal.saveAll(folderList)
            }
        }

        fun setOnFindCallback(callBack: FindCallBack){
            mCallBack = callBack
        }

        //用于异步的find回调
        interface FindCallBack{
            fun onFind(folder: MyFolder, folderList: ArrayList<MyFolder>)
            fun onFindStarFiles(starFileList: ArrayList<MyFile>)
        }
    }
}