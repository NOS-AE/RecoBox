package org.fmod.recobox.activity

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_cloud.*
import org.fmod.recobox.R
import org.fmod.recobox.adapter.CloudListAdapter
import org.fmod.recobox.bean.MyFile
import org.fmod.recobox.manager.BackEndManager
import org.fmod.recobox.util.FileUtil

class CloudActivity : BaseActivity() {
    private lateinit var progress: ProgressBar
    private lateinit var filename: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud)

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorTheme)

        BackEndManager.setOnFileCallback(object : BackEndManager.FileCallback{
            override fun onComplete(fileList: ArrayList<MyFile>) {
                runOnUiThread {
                    setRecyclerView(fileList)
                }
            }

            override fun onDownloadUpdate(percent: Int) {
                //更新进度条
                Log.d(TAG,percent.toString())
                progress.progress = percent
            }

            override fun onDownloadComplete(data: ByteArray) {
                FileUtil.bytesToFile(data,filename)
            }
        })

        BackEndManager.getFileInfo()
        setListener()
    }

    private fun setRecyclerView(fileList: ArrayList<MyFile>){
        cloud_list.layoutManager = LinearLayoutManager(this)
        val adapter = CloudListAdapter(fileList,BaseActivity.nickname, BaseActivity.cloudContent)
        adapter.setOnItemClickListener(object : CloudListAdapter.OnItemClickListener{
            override fun clickDownload(progress: View,filename: String) {
                this@CloudActivity.filename = filename
                BackEndManager.downLoadFile(filename)
                this@CloudActivity.progress = progress as ProgressBar
            }
        })
        cloud_list.adapter = adapter
    }

    private fun setListener(){
         return_record.setOnClickListener {
             finish()
         }

        /*open_download.setOnClickListener {

        }*/
    }
}
