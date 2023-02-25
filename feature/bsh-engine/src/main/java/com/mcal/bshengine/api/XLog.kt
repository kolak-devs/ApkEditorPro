package com.mcal.bshengine.api

import androidx.recyclerview.widget.RecyclerView
import com.mcal.bshengine.adapters.LogAdapter
import com.mcal.common.data.Constants
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XLog(private val adapter: ItemAdapter<LogAdapter>, private val recyclerView: RecyclerView, private val fastApkAdapter: FastAdapter<LogAdapter>) {
    /**
     * Since: 2.4.5
     */
    fun info(message: String) {
        info(message, false)
    }

    /**
     * 2.4.6
     */
    fun info(message: String, boldStyle: Boolean) {
        if (message.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                adapter.add(
                    LogAdapter().withId(message.hashCode().toLong()).withLogLevel(Constants.LOG_INFO).withBold(boldStyle).withLogString(message)
                )
                recyclerView.smoothScrollToPosition(fastApkAdapter.itemCount)
            }
        }
    }

    /**
     * Since: 2.4.5
     */
    fun error(message: String) {
        error(message, false)
    }

    /**
     * 2.4.6
     */
    fun error(message: String, boldStyle: Boolean) {
        if (message.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                adapter.add(
                    LogAdapter().withId(message.hashCode().toLong()).withLogLevel(Constants.LOG_ERROR).withBold(boldStyle).withLogString(message)
                )
                recyclerView.smoothScrollToPosition(fastApkAdapter.itemCount)
            }
        }
    }

    fun clear() {
        CoroutineScope(Dispatchers.Main).launch {
            adapter.clear()
        }
    }
}