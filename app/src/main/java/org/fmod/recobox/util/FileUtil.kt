package org.fmod.recobox.util

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.support.v4.content.FileProvider
import android.util.Log
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.bean.MyFolder
import org.litepal.LitePal
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

//文件管理
class FileUtil{
    companion object {

        private val appPath = "${Environment.getExternalStorageDirectory().absolutePath}/RecoSound"
        val soundAudioPath = "$appPath/sound"
        val tempAudioPath = "$appPath/temp"
        private val tempMp3Path = "$appPath/tempMp3"

        private const val SHARE_AUTHORITY = "org.fmod.racobox.fileprovider"
        private var encodeListener: EncodeListener? = null

        init {
            //创立文件夹
            var file = File(tempAudioPath)
            if(!file.exists()){
                file.mkdirs()
            }
            file = File(soundAudioPath)
            if(!file.exists()){
                file.mkdirs()
            }
            file = File(tempMp3Path)
            if(!file.exists()){
                file.mkdirs()
            }
        }

        fun shareFile(filename: String, context: Context){
            thread {
                //转码成mp3，需要一定时间，开启线程，完成后回调
                val mp3File = "$tempMp3Path/$filename.mp3"
                AudioUtil.pcmToMp3("$soundAudioPath/$filename.pcm",mp3File)
                encodeListener?.onComplete()
                //分享文件
                val file = File(mp3File)
                val share = Intent(Intent.ACTION_SEND)
                share.run {
                    putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(context, SHARE_AUTHORITY,file))
                    type = getMinType(file.absolutePath)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(share,"分享文件"))
                }
            }
        }

        fun shareMultiFile(fileList: ArrayList<String>, context: Context){
            thread {
                var mp3File: String
                //转码成MP3
                val uriList = ArrayList<Uri>()
                for (i in fileList) {
                    mp3File = "$tempMp3Path/$i.mp3"
                    AudioUtil.pcmToMp3(
                        "$soundAudioPath/$i.pcm",
                        mp3File
                    )
                    uriList.add(FileProvider.getUriForFile(context, SHARE_AUTHORITY, File(mp3File)))
                }
                encodeListener?.onComplete()
                val minType = getMinType("$tempMp3Path/${fileList[0]}.mp3")
                //分享文件
                val share = Intent(Intent.ACTION_SEND_MULTIPLE)
                share.run {
                    type = minType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM,uriList)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(this,"分享文件"))
                }
            }
        }

        private fun getMinType(filePath: String): String{
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        }

        fun mergeFiles(amount: Int, filename: String, desert: Boolean) {
            //desert:是否合并
            //合并临时文件
            val fos: FileOutputStream? = if (desert) null else FileOutputStream("$soundAudioPath/$filename.pcm")
            var fis: FileInputStream
            var file: File
            val readBufferSize = 8192
            val buffer = ByteArray(readBufferSize)
            for (i in 1 until amount) {
                //遍历临时文件
                file = File("$tempAudioPath/$i.pcm")
                Log.d("MyApp",file.name)
                if (!desert) {
                    fis = FileInputStream(file)
                    //去除静音部分(粗略)
                    fis.skip(readBufferSize * 4L)
                    /*for(j in 1..6){
                        if(fis.available() > 0){
                            fis.read(buffer,0,readBufferSize)
                        }
                    }*/
                    while (fis.available() > 0) {
                        fis.read(buffer, 0, buffer.size)
                        fos?.write(buffer, 0, buffer.size)
                    }
                    fis.close()
                }
                //已完成或不需合并的文件，删除
                file.delete()
            }
            fos?.close()
        }

        fun deleteFiles(fileList: ArrayList<MyFile>){
            var file: File
            for(i in fileList){
                if(i.isCheck) {
                    file = File("$soundAudioPath/${i.filename}.pcm")
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }

        fun deleteFile(filename: String){
            val file = File("$soundAudioPath/$filename.pcm")
            if (file.exists())
                file.delete()
        }

        fun deleteTempMp3(){
            val file = File(tempMp3Path)
            Log.d("MyApp", tempMp3Path)
            val list = file.listFiles()
            list?:return
            for(i in list){
                i.delete()
            }
        }

        fun renameFile(old:String, new: String){
            val from = File("$soundAudioPath/$old.pcm")
            val to = File("$soundAudioPath/$new.pcm")
            if(from.exists()){
                from.renameTo(to)
            }
        }

        fun setOnEncodeListener(listener: EncodeListener){
            encodeListener = listener
        }

        /*fun setEncodeListener(listener: EncodeListener){
            encodeListener = listener
        }*/

        fun bytesToFile(data: ByteArray, filename: String){
            val fos = FileOutputStream("$soundAudioPath/$filename")
            fos.write(data)
        }
    }





    //mp3转码完成监听
    interface EncodeListener{
        fun onComplete()
    }
}