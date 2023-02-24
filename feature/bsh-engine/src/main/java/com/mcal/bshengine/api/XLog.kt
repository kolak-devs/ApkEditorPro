package com.mcal.bshengine.api

import androidx.recyclerview.widget.RecyclerView
import com.mcal.bshengine.adapters.LogAdapter
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XLog(private val adapter: ItemAdapter<LogAdapter>, private val recyclerView: RecyclerView, private val fastApkAdapter: FastAdapter<LogAdapter>) {
    fun info(message: String) {
        if (message.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                adapter.add(
                    LogAdapter()
                        .withId((0..Integer.MAX_VALUE).random().toLong())
                        .withTitle(message)
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