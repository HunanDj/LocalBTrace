package com.jay.localtrace

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.jay.localtrace.utils.TraceDownloadUtils.Companion.downloadTraceFiles
import com.jay.localtrace.utils.TraceLogger
import com.jay.localtrace.utils.TraceProcessUtils.Companion.startDumpTrace
import com.jay.localtrace.utils.TraceProcessUtils.Companion.stopDumpTrace
import java.io.File


const val TAG = "LocalTrace"

class LocalTraceSession private constructor(
    private val methodIdMaxSize: Long,
    private val duration: Int,
    private val mainThreadOnly: Boolean,
    private val outputPath: File?
) {
    class Builder {
        private var methodIdMaxSize: Long = 1024
        private var duration: Int = 0
        private var mainThreadOnly: Boolean = false
        private var outputPath: File? = null

        fun setMethodIdMaxSize(methodIdMaxSize: Long): Builder {
            this.methodIdMaxSize = methodIdMaxSize
            return this
        }

        fun setDuration(duration: Int): Builder {
            this.duration = duration
            return this
        }

        fun setMainThreadOnly(mainThreadOnly: Boolean): Builder {
            this.mainThreadOnly = mainThreadOnly
            return this
        }

        fun setOutputPath(outputPath: File): Builder {
            this.outputPath = outputPath
            return this
        }

        fun build(): LocalTraceSession {
            return LocalTraceSession(methodIdMaxSize, duration, mainThreadOnly, outputPath)
        }
    }

    companion object {
        var taskRunningFlag: Boolean = false
    }


    private val taskScheduler = Handler(Looper.getMainLooper())


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun start(context: Context, onComplete: (Boolean, String?) -> Unit) {

        if (taskRunningFlag) {
            onComplete(false, "the dump trace task is running! please wait it done!")
            return
        }

        taskRunningFlag = true
        //first 开始
        TraceLogger.d(TAG, "start dump ...")
        startDumpTrace(context)

        //second 结束
        val stopTask = {
            stopDumpTrace(context)
            TraceLogger.d(TAG, "end dump ...")
        }
        taskScheduler.postDelayed(stopTask, duration.toLong())

        //third 下载产物
        //稍微等一下再下载似乎可以避免偶先的下载失败问题
        val time = duration.toLong() + 100
        val listener: (Boolean, String?) -> Unit = { result, msg ->
            onComplete(result, msg)
            taskRunningFlag = false
        }
        val downloadTraceTask = { downloadTraceFiles(context, outputPath, listener) }
        taskScheduler.postDelayed(downloadTraceTask, time)
    }

}

