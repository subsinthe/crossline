package com.example.subsinthe.crossline

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import com.example.subsinthe.crossline.streaming.IStreamingService.MusicTrack
import com.example.subsinthe.crossline.util.IObservableList
import com.example.subsinthe.crossline.util.MappingOp
import com.example.subsinthe.crossline.util.loggerFor
import java.io.Closeable

class SearchModel(
    source: IObservableList<MusicTrack>
) : RecyclerView.Adapter<SearchModel.Holder>(), Closeable {
    private val storage = ArrayList<MusicTrack>()
    private val connection = source.subscribe { onSourceChanged(it) }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val artistView = view.findViewById<TextView>(R.id.artist)
        val titleView = view.findViewById<TextView>(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.search_result, parent, false
        ) as View
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val musicTrack = storage[position]
        holder.artistView.text = musicTrack.artist
        holder.titleView.text = musicTrack.title
    }

    override fun getItemCount() = storage.size

    override fun close() = connection.close()

    private fun onSourceChanged(op: MappingOp<Int, MusicTrack>) {
        LOG.fine("onSourceChanged($op)")

        when (op) {
            is MappingOp.Added -> {
                storage.add(op.key, op.value)
                notifyItemInserted(op.key)
            }
            is MappingOp.Removed -> {
                storage.removeAt(op.key)
                notifyItemRemoved(op.key)
            }
            is MappingOp.Updated -> {
                storage.set(op.key, op.new)
                notifyItemChanged(op.key)
            }
        }
    }

    private companion object { val LOG = loggerFor<SearchModel>() }
}
