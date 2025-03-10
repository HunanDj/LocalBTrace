package com.jay.localtrace.utils

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.bytedance.rheatrace.atrace.BinaryTrace
import com.bytedance.rheatrace.core.HttpServer
import com.jay.localtrace.TAG
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.lang.reflect.Field
import java.net.URL

class TraceDownloadUtils {

    companion object {
        /***
         * 反射获取http服务的端口
         * */
        private fun getServerInstance(): NanoHTTPD? {
            return try {
                val httpServerClass = HttpServer::class.java
                val serverField: Field = httpServerClass.getDeclaredField("server")
                serverField.isAccessible = true
                serverField.get(null) as NanoHTTPD? // 传 null，因为是 static
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun notifyInMainThread(
            result: Boolean,
            msg: String?,
            onComplete: (Boolean, String?) -> Unit
        ) {
            Handler(Looper.getMainLooper()).post {
                onComplete(result, msg)
            }
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        fun downloadTraceFiles(
            context: Context,
            path: File? = null,
            onComplete: (Boolean, String?) -> Unit
        ) {
            TraceLogger.d(TAG, "start download downloadTraceFiles!")

            val port = getServerInstance()?.listeningPort ?: -1
            if (port == -1) {
                notifyInMainThread(false, "未检测到下载端口，无法下载trace文件", onComplete)
                return
            } else {
                TraceLogger.d(TAG, "检测到下载端口:$port")
            }
            // 检查外接存储器是否可用
            val externalStorageDirs = context.getExternalFilesDirs(null)

            externalStorageDirs.forEach {
                TraceLogger.d(TAG, "find externalStorageDirs = " + it.absolutePath)
            }

            val externalStorage = externalStorageDirs.last()
            val traceDir: File? =
                path ?: if (externalStorage != null) {
                    // 外接存储器根目录下的 trace 文件夹
                    File(externalStorage, "trace")
                } else {
                    null
                }

            // 创建 trace 目录
            if (traceDir == null || (!traceDir.exists() && !traceDir.mkdirs())) {
                notifyInMainThread(false, "无法创建 trace 目录", onComplete)
                return
            }

            TraceLogger.d(TAG, "download path  ${traceDir.absolutePath}!")
            Thread {
                try {
                    // 下载文件到指定目录
                    val files = listOf(
                        "trace" to File(traceDir, "appTrace.bin"),
                        "rhea-atrace.gz" to File(traceDir, "rhea-atrace.gz"),
                        "binder.txt" to File(traceDir, "binder.txt")
                    )

                    for ((name, destination) in files) {
                        val url = "http://localhost:$port?name=$name"
                        TraceLogger.d(TAG, "download file  $name to $destination! with URL $url")

                        val hostDir = File(context.getExternalFilesDir(""), "rhea-trace");
                        if ("trace" == name) {
                            copyFileSegment(
                                File(hostDir, "rhea-atrace.bin").absolutePath,
                                destination,
                                BinaryTrace.currentBufferUsage()
                            )
                        } else {
                            copyFile(File(hostDir, name), destination)
                        }
                    }
                    TraceLogger.d(TAG, "download downloadTraceFiles success!")
                    // 在主线程显示下载成功的 Toast
                    notifyInMainThread(true, "下载 trace 文件成功！", onComplete)
                } catch (e: Exception) {
                    // 在主线程显示下载失败的 Toast
                    TraceLogger.d(
                        TAG,
                        "download downloadTraceFiles failed ${Log.getStackTraceString(e)}"
                    )
                    notifyInMainThread(true, "下载 trace 文件失败：${e.message}", onComplete)
                }
            }.start()
        }


        private fun copyFileSegment(sourceFile: String, destFile: File, length: Long) {
            RandomAccessFile(sourceFile, "r").use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var remaining = length

                    while (remaining > 0) {
                        bytesRead = input.read(buffer, 0, minOf(buffer.size, remaining.toInt()))
                        if (bytesRead == -1) break  // 读到文件结尾
                        output.write(buffer, 0, bytesRead)
                        remaining -= bytesRead
                    }
                }
            }
        }


        private fun copyFile(sourceFile: File, destFile: File) {
            try {
                sourceFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to copy file: ${sourceFile.absolutePath}")
                e.printStackTrace()
            }
        }

    }


}