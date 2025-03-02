package com.jay.localtrace.utils

import android.content.Context
import android.content.Intent
import com.bytedance.rheatrace.core.TraceReceiver

class TraceProcessUtils {

    companion object {

        fun startDumpTrace(context: Context) {
            context.sendBroadcast(
                Intent(
                    context,
                    TraceReceiver::class.java
                ).setAction("com.bytedance.rheatrace.switch.start")
            )
        }


        fun stopDumpTrace(context: Context) {
            context.sendBroadcast(
                Intent(
                    context,
                    TraceReceiver::class.java
                ).setAction("com.bytedance.rheatrace.switch.stop")
            )
        }
    }

}