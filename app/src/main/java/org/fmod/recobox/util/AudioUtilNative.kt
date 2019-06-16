package org.fmod.racobox.util

//转码，计算频率
class AudioUtilNative{
    //初始化encoder
    external fun init(
        pcmPath: String,
        channels: Int,
        bitRate: Int,
        sampleRate: Int,
        mp3Path: String): Int
    //转码
    external fun encode()
    //回收encoder
    external fun destroy()
    //计算sample频率
    external fun processSampleData(buffer: ByteArray, sampleRate: Int): Double
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}