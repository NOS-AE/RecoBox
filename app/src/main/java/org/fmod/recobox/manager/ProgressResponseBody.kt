package org.fmod.recobox.manager


import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

class ProgressResponseBody(
    private val responseBody: ResponseBody?,
    private val progressListener: ProgressListener
): ResponseBody(){

    private lateinit var bufferedSource: BufferedSource

    override fun contentLength(): Long {
        return responseBody!!.contentLength()
    }

    override fun contentType(): MediaType? {
        return responseBody?.contentType()
    }

    override fun source(): BufferedSource {
        if(!this::bufferedSource.isInitialized)
            bufferedSource = Okio.buffer(source(responseBody!!.source()))
        return bufferedSource
    }

    private fun source(source: Source): Source{
        return object : ForwardingSource(source){
            private var totalBytesRead = 0L
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                if(bytesRead != -1L)
                    totalBytesRead += bytesRead
                //接口回调
                progressListener.update(totalBytesRead,responseBody!!.contentLength(),bytesRead == -1L)
                return bytesRead
            }
        }
    }

    //回调接口
    interface ProgressListener{
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }
}