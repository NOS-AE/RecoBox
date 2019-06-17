package org.fmod.recobox.manager


import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.util.FileUtil
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.text.StringBuilder

//后端相关
//QQ登录
/*
* QQ登录获取用户信息后，直到退出登录才再次进行QQ登录
*
* */
class BackEndManager{
    companion object {
        private const val TAG = "MyApp"

        private const val host = "http://47.102.118.1:520"
        private const val loginUrl = "http://47.102.118.1:520/api_1_0/user"
        private const val fileInfoUrl = "http://47.102.118.1:520/api_1_0/file_info"
        private const val fileUrl = "http://47.102.118.1:520/api_1_0/file"
        private const val infoUrl = "http://47.102.118.1:520/api_1_0/user_info"
        private const val usernameUrl = "http://47.102.118.1:520/api_1_0/username"

        private val JPG = MediaType.parse("application/x-jpg")
        private val JSON = MediaType.parse("application/json; charset=utf-8")
        private val FILE = MediaType.parse("application/octet-stream")

        private val client = OkHttpClient()
        private val gson = Gson()

        private lateinit var account: String//access_token
        private lateinit var token: String//后端token

        private var userInfoCallback: UserInfoCallback? = null
        private var fileInfoCallback: FileCallback? = null

        fun login(account: String){
            this.account = account
            val url = "$loginUrl?account=$account"
            val request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).enqueue(object :Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"Backend login fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d(TAG,"Backend login successful")
                        val json = response.body()!!.string()
                        val jsonObject = JSONObject(json)
                        token = jsonObject.getString("token")
                        //因为account已存在，所以肯定不是第一次登录
                        getUserInfo()
                    }
                    else{
                        Log.d(TAG,"Backend login unsuccessful: ${response.body()?.string()}")
                    }
                }
            })
        }

        fun login(account: String, avatar: String, nickname: String){
            this.account = account
            val url = "$loginUrl?account=$account"
            val request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).enqueue(object :Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"Backend login fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d(TAG,"Backend login successful")
                        val json = response.body()!!.string()
                        val jsonObject = JSONObject(json)
                        token = jsonObject.getString("token")
                        //第一次登录，用户账号信息默认为qq账号信息
                        if(jsonObject.getInt("status") == 201) {
                            uploadUserInfo(avatar, nickname)
                            //回调，从qq获取的头像和昵称
                            userInfoCallback?.onComplete(avatar,nickname,100.0)
                        }
                        else{
                            //不是第一次登录，获取头像和昵称
                            getUserInfo()
                        }
                        Log.d(TAG,json)
                        /*Log.d(TAG, cloudBean.data.toString())*/
                    }
                    else{
                        Log.d(TAG,"Backend login unsuccessful: ${response.body()?.string()}")
                    }
                }
            })
        }

        //从后端获取用户信息
        private fun getUserInfo(){
            val url = "$infoUrl?token=$token"
            val request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"Backend getUserInfo fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful) {
                        Log.d(TAG, "Backend getUserInfo successful")
                        val json = JSONObject(response.body()?.string())
                        //回调
                        val avatar = host + json.getString("avatar")
                        val nickname = json.getString("username")
                        val size = json.getDouble("size")
                        userInfoCallback?.onComplete(avatar, nickname,size)
                        Log.d(TAG,"$avatar, $nickname")
                    }else{
                        Log.d(TAG,"Backend getUserInfo unsuccessful: ${response.body()?.string()}")
                    }
                }
            })
        }

        private fun uploadUserInfo(avatar: String, nickname: String){
            //获取头像
            val request = Request.Builder()
                .url(avatar)
                .build()
            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"Get avatar from qq fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful){
                        Log.d(TAG,"Get avatar from qq successful")
                        //将获取到的头像和传入的昵称提交到后端
                        uploadInfo(byte2Image(response.body()!!.bytes()),nickname,true)
                    }
                    else
                        Log.d(TAG,"Get avatar from qq unsuccessful: ${response.body()?.string()}")
                }
            })
        }

        fun uploadNickname(nickname: String){
            val json = "{\"token\":\"$token\",\"username\":\"$nickname\"}"
            val requestBody = RequestBody.create(JSON,json)
            val request = Request.Builder()
                .url(usernameUrl)
                .post(requestBody)
                .build()
            client.newCall(request).enqueue(object :Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"uploadNickname fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful){
                        Log.d(TAG,"uploadNickname successful: ${response.body()?.string()}")
                    }else{
                        Log.d(TAG,"uploadNickname unsuccessful: ${response.body()?.string()}")
                        //不作其它处理..
                    }
                }
            })
        }

        fun uploadInfo(avatar: File, nickname: String, first: Boolean = false){
            val fileBody = RequestBody.create(JPG,avatar)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("avatar","$account.jpg",fileBody)
                .addFormDataPart("username",nickname)
                .addFormDataPart("token", token)
                .build()

            val request = Request.Builder()
                .url(infoUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"Upload info fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful){
                        Log.d(TAG,"Upload info successful: ${response.body()?.string()}")
                        if(first) {
                            //删除暂存的头像
                            avatar.delete()
                        }
                    }
                    else{
                        Log.d(TAG,"Upload info unsuccessful: ${response.body()?.string()}")
                    }
                }
            })
        }

        fun getFileInfo(){
            val url = "$fileInfoUrl?token=$token"
            val request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"getFileInfo fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful){
                        val json = response.body()!!.string()
                        val jsonObject = JSONObject(json)

                        val status = jsonObject.getInt("status")
                        Log.d(TAG,"getFileInfo successful: $status")
                        if(status == 200) {
                            val jsonArray = jsonObject.getJSONArray("data")
                            val gson = Gson()
                            val fileList: ArrayList<MyFile> = gson.fromJson(jsonArray.toString())
                            fileInfoCallback?.onComplete(fileList)
                        }
                    }else{
                        Log.d(TAG,"getFileInfo unsuccessful")
                    }
                }
            })
        }

        fun downLoadFile(filename: String){
            val url = "$fileUrl?filename$filename&token=$token"
            val request = Request.Builder()
                .url(url)
                .build()

            //进度监听器
            val listener = object :ProgressResponseBody.ProgressListener{
                override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                    //max=100 min=1
                    val percent = bytesRead / contentLength * 100
                    fileInfoCallback?.onDownloadUpdate(percent.toInt())
                }
            }

            //添加拦截器
            val client = OkHttpClient.Builder()
                .addNetworkInterceptor {
                    val response = it.proceed(it.request())
                    //换成ProgressResponseBody
                    response.newBuilder()
                        .body(ProgressResponseBody(response.body(), listener))
                        .build()
                }
                .build()

            //发送请求
            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"downloadFile fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful) {
                        Log.d(TAG, "downloadFile successful")
                        fileInfoCallback?.onDownloadComplete(response.body()!!.bytes())
                    }else{
                        Log.d(TAG,"downloadFile unsuccessful: ${response.body()?.string()}")
                    }
                }
            })
        }

        private fun byte2Image(bytes: ByteArray): File{
            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/avatar_temp.jpg")
            try {
                val fos = FileOutputStream(file)
                fos.write(bytes, 0, bytes.size)
                fos.flush()
                fos.close()
            }catch (e: IOException) {
                Log.d(TAG, "byte2Image fail: $e")
            }
            return file
        }

        /**
         * 上传文件
         * @param files 一批文件的文件名
         * */
        fun uploadFiles(bean: MyFile){
            val file = File(FileUtil.soundAudioPath + "/${bean.filename}.pcm")
            val fileBody = RequestBody.create(FILE,file)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("files",file.name,fileBody)
                .addFormDataPart("token", token)
                .addFormDataPart("date",bean.date)
                .addFormDataPart("duration",bean.duration.toString())
                .addFormDataPart("description",bean.description)
                .build()

            val request = Request.Builder()
                .url(fileUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG,"uploadFiles fail: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful){
                        Log.d(TAG,"uploadFiles successful: ${response.body()?.string()}")
                    }else{
                        Log.d(TAG,"uploadFiles unsucccessful: ${response.body()?.string()}")
                    }
                }
            })
        }


        /*/**
         * 下载文件(多次GET)
         * @param files 一批文件的文件名
         * */
        fun downloadFiles(fileList: List<String>){

        }*/

        /*/**
         * 删除文件(多次DELETE)
         * @param files 一批文件的文件名
         * */
        fun deleteFiles(fileList: List<String>){

        }*/



        /*/**
         * 将字符串转base64
         * @param str 要转成base64的字符串
         * */
        private fun str2Base64(str: String): String{
            return Base64.getEncoder().encodeToString(str.toByteArray())
        }*/

        /*/**
         * 将文件转md5
         * @param file 要转成md5的文件
         * */
        private fun file2Md5(file: File): String{
            //获取MD5对象单例
            val MD5 = MessageDigest.getInstance("MD5")
            //将文件转逐块转成MD5
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var length = fis.read(buffer)
            while (length != -1){
                MD5.update(buffer,0,length)
                length = fis.read(buffer)
            }
            return bytes2Hex(MD5.digest())
        }*/

        /**
         * 将byteArray转字符串形式的16进制
         * @param arr 要转成16进制字符串的byteArray
         * */
        private fun bytes2Hex(arr: ByteArray): String{
            val sb = StringBuilder()
            for(b in arr){
                sb.append(String.format("%02x",b))
            }
            return sb.toString()
        }

        private inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

        fun setOnLoginCallback(callback: UserInfoCallback) {
            userInfoCallback = callback
        }
        fun setOnFileCallback(listener: FileCallback){
            fileInfoCallback = listener
        }

    }



    //获取用户信息完成回调
    interface UserInfoCallback{
        fun onComplete(avatar: String, nickname: String, size: Double)
    }

    //获取文件信息完成回调
    interface FileCallback{
        fun onComplete(fileList: ArrayList<MyFile>)
        fun onDownloadUpdate(percent: Int)
        fun onDownloadComplete(data: ByteArray)
    }
}