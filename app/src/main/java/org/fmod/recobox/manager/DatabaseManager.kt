package org.fmod.recobox.manager


import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.litepal.LitePal

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
        fun findCurrentList(parentId: Long){
            //开始查询
            // 加载当前目录，获取文件关联表
            LitePal.findAsync(MyFolder::class.java, parentId,true).listen {
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
            /*LitePal.where("level = ?", "$level")
                .where("levelId = ?","$levelId")
                .findAsync(MyFolder::class.java,true).listen {
                    if(it.isNotEmpty()) {
                        folderList.add(it[0])//level和levelId确定一个folder
                    }
                    if (isAnotherFind) {
                        mCallBack?.onFind(folderList)
                    }
                    isAnotherFind = !isAnotherFind
                }
            LitePal.where("level = ?","${level + 1}")
                .findAsync(MyFolder::class.java).listen {
                    folderList.addAll(0,it)//folder列表添加到最前面
                    if(isAnotherFind){
                        mCallBack?.onFind(folderList)
                    }
                    isAnotherFind = !isAnotherFind
                }*/
        }

        fun findStarFile(){
            LitePal.where("star = ?", "1")
                .findAsync(MyFile::class.java).listen {
                    mCallBack?.onFindStarFiles(it as ArrayList<MyFile>)
                }
        }

        fun findRootFolder(): MyFolder{
            return LitePal.find(MyFolder::class.java,1,true)
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