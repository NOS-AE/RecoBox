package org.fmod.recobox.util

import android.media.*
import android.util.Log
import org.fmod.recobox.util.FileUtil.Companion.soundAudioPath
import org.fmod.recobox.util.FileUtil.Companion.tempAudioPath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

//录音，播放
class AudioUtil{
    companion object {
        private const val TAG = "MyApp"

        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 2048
        private const val RECORD_CHANNEL = AudioFormat.CHANNEL_IN_STEREO
        private const val PLAY_CHANNEL = AudioFormat.CHANNEL_OUT_STEREO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val RECORD_RECORDING = 1
        private const val RECORD_PAUSE = 2
        private const val RECORD_STOP = 3

        private val playerMinBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, PLAY_CHANNEL,
            AUDIO_ENCODING)

        private lateinit var recorder: AudioRecord
        private lateinit var player: AudioTrack
        private val encoder = AudioUtilNative()
        private var recordState = RECORD_STOP

        private var tempFileId = 1

        private lateinit var filename: String

        private var mListener: PlayListener? = null

        init {
            tempFileId = 1
        }

        fun setPlayListener(listener: PlayListener){
            mListener = listener
        }

        fun startRecord(filename: String){
            Log.d(TAG,"startRecord")
            recordState = RECORD_RECORDING
            this.filename = filename
            //开启线程录音，生成临时文件
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, RECORD_CHANNEL, AUDIO_ENCODING, BUFFER_SIZE)
            recorder.startRecording()
            thread {
                writeAudioDataToFile("$tempAudioPath/${tempFileId++}.pcm")
            }
        }

        fun pauseRecord(){
            Log.d(TAG,"pauseRecord")
            //暂停录音
            recordState = RECORD_PAUSE
            recorder.stop()
            recorder.release()
        }

        fun resumeRecord(){
            Log.d(TAG,"resumeRecord")
            //开启线程录音，生成新临时文件
            recordState = RECORD_RECORDING
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, RECORD_CHANNEL, AUDIO_ENCODING, BUFFER_SIZE)
            //setVisualizer()
            recorder.startRecording()
            thread {
                writeAudioDataToFile("$tempAudioPath/${tempFileId++}.pcm")
            }
        }

        fun stopRecord(){
            Log.d(TAG,"stopRecord")
            //结束录音，重置所有状态
            recordState = RECORD_STOP
            if(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
                recorder.release()
            }
        }

        fun createAudioFile(desert: Boolean,filename: String = ""){
            //合并最终文件
            thread {
                //合并文件
                FileUtil.mergeFiles(tempFileId, filename,desert)
                //重置第一个临时文件的ID
                tempFileId = 1
            }
        }

        /**
         * @param filename 文件名
         * @param percent 从百分之percent开始播放
         * */
        fun playMusic(filename: String, percent: Float = 0f, end: Float = 1f){
            this.filename = filename
            Log.d(TAG,"playPause")
            thread {
                //从percent开始播放
                val fis = FileInputStream("$soundAudioPath/$filename.pcm")
                val skipByte = (fis.available() * percent).toLong()
                val allReadBytes = (fis.available() * end - skipByte).toInt()
                var countByte = 0
                fis.skip(skipByte)
                //创建AudioTrack
                if(this::player.isInitialized && player.playState != AudioTrack.PLAYSTATE_STOPPED)
                    player.stop()
                player = AudioTrack(
                    AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build(),
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AUDIO_ENCODING)
                        .setChannelMask(PLAY_CHANNEL)
                        .build(),
                    playerMinBufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                //创建buffer
                val buffer = ByteArray(playerMinBufferSize)
                var readCount: Int
                //开始播放
                player.play()
                while (fis.available() > 0){
                    if(player.playState == AudioTrack.PLAYSTATE_PLAYING){
                        readCount = fis.read(buffer)
                        if(readCount == AudioTrack.ERROR_BAD_VALUE || readCount == AudioTrack.ERROR_INVALID_OPERATION)
                            continue
                        if(readCount != 0 && readCount != -1)
                            player.write(buffer,0,readCount)
                        countByte+=readCount
                        if(countByte >= allReadBytes)
                            stopMusic()
                    }else if(player.playState == AudioTrack.PLAYSTATE_STOPPED){
                        break
                    }
                }
                player.stop()
                mListener?.onStop()
            }
        }

        fun pauseMusic(){
            Log.d(TAG,"pauseMusic")
            if(this::player.isInitialized && player.playState == AudioTrack.PLAYSTATE_PLAYING)
                player.pause()
        }

        fun stopMusic(){
            Log.d(TAG,"stopMusic")
            if(this::player.isInitialized && player.playState != AudioTrack.PLAYSTATE_PLAYING)
                player.stop()
        }

        private fun writeAudioDataToFile(path: String){
            //write the output audio in byte
            val data = ByteArray(BUFFER_SIZE)
            val os = FileOutputStream(path)
            while(recordState == RECORD_RECORDING){
                //gets the voice output from microphone to byte format
                //recorder从外界读入数据到data
                recorder.read(data, 0, BUFFER_SIZE)
                //写到文件
                os.write(data, 0, BUFFER_SIZE)
                //TODO 计算此sample音频频率
                /*rate = util.processSampleData(data, SAMPLE_RATE)
                if(rate > 500 && rate < 2500)
                    Log.d("AudioUtil",rate.toString())*/
            }
            os.close()
        }

        fun pcmToMp3(pcmFile: String, mp3File: String){
            if(encoder.init(pcmFile, RECORD_CHANNEL, 128, SAMPLE_RATE, mp3File) == 0){
                Log.d(TAG,"pcmToMp3 start")
                encoder.encode()
                encoder.destroy()
                Log.d(TAG,"pcmToMp3 finish")
            }else{
                Log.d(TAG,"pcmToMp3 init fail")
            }
        }

        /*fun clipRecord(filename: String, p1: Float, p2: Float){
            val data = ByteArray(BUFFER_SIZE)
            val file = File(FileUtil.soundAudioPath + "/" + filename + ".pcm")

            val fos = FileOutputStream(file)
            val fis = FileInputStream(audioPath)
            var times = fis.available() / 2 / BUFFER_SIZE
            while((times--) > 0){
                fis.read(data,0, BUFFER_SIZE)
                fos.write(data)
            }
            fos.close()
            fis.close()
        }*/

    }
    interface PlayListener{
        fun onStop()
    }

}