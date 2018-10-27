package com.example.subsinthe.crossline

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class LoadMoreScrollListener(
    private val layoutManager: LinearLayoutManager
) : RecyclerView.OnScrollListener() {
    private val loadMore = Channel<Unit>()

    val channel: ReceiveChannel<Unit> = loadMore

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (layoutManager.findLastVisibleItemPosition() == (layoutManager.getItemCount() - 1))
            loadMore.offer(Unit)
    }
}
