package com.jay.localtrace

import android.content.Context
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
    private val duration: Int,
    private val outputPath: File?
) {
    class Builder {
        private var duration: Int = 0
        private var outputPath: File? = null

        fun setMethodIdMaxSize(methodIdMaxSize: Long): Builder {
            HookConfig.setMethodIdMaxSize(methodIdMaxSize)
            return this
        }

        fun setDuration(duration: Int): Builder {
            this.duration = duration
            return this
        }

        fun setMainThreadOnly(mainThreadOnly: Boolean): Builder {
            HookConfig.setMainThreadOnly(mainThreadOnly)
            return this
        }

        fun setOutputPath(outputPath: File): Builder {
            this.outputPath = outputPath
            return this
        }

        fun build(): LocalTraceSession {
            return LocalTraceSession(duration, outputPath)
        }
    }

    companion object {
        var taskRunningFlag: Boolean = false
        var hasInit: Boolean = false

        init {
            TraceLogger.d(TAG, "localTrace so init start!")
            System.loadLibrary("localTrace")
            hasInit = true
            TraceLogger.d(TAG, "localTrace so init success!")
        }
    }


    private val taskScheduler = Handler(Looper.getMainLooper())


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun start(context: Context, onComplete: (Boolean, String?) -> Unit) {
        if (!hasInit) {
            TraceLogger.d(TAG, "localTrace so init failed!")
            onComplete(false, "localTrace so init failed!")
            return
        }
        if (taskRunningFlag) {
            onComplete(false, "the dump trace task is running! please wait it done!")
            return
        }

        val sdkVersion = Build.VERSION.SDK
        TraceLogger.d(TAG, "SDK Version: $sdkVersion")

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

