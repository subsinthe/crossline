package com.example.subsinthe.crossline

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import android.widget.ProgressBar
import com.example.subsinthe.crossline.streaming.MusicTrack
import com.example.subsinthe.crossline.util.IObservableList
import com.example.subsinthe.crossline.util.IObservable
import com.example.subsinthe.crossline.util.MappingOp
import com.example.subsinthe.crossline.util.Token
import com.example.subsinthe.crossline.util.discard
import com.example.subsinthe.crossline.util.loggerFor
import java.io.Closeable

class SearchModel(
    source: IObservableList<MusicTrack>,
    private val isSearchActive: IObservable<Boolean>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Closeable {
    private val storage = ArrayList<MusicTrack>()
    private val sourceConnection = source.subscribe { onSourceChanged(it) }
    private var isSearchActiveConnection = Token()

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artistView = view.findViewById<TextView>(R.id.artist)
        val titleView = view.findViewById<TextView>(R.id.title)
    }

    class ProgressHolder(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < storage.size) ViewType.Item.code else ViewType.Progress.code
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        ViewType.Item.code -> {
            ItemHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.search_result, parent, false
                ) as View
            )
        }
        ViewType.Progress.code -> {
            ProgressHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.progress_item, parent, false
                ) as View
            )
        }
        else -> { throw IllegalArgumentException("Unexpected view type $viewType") }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = when (holder) {
        is ItemHolder -> {
            val musicTrack = storage[position]
            holder.artistView.text = musicTrack.artist
            holder.titleView.text = musicTrack.title
        }
        is ProgressHolder -> {
            isSearchActiveConnection.close()

            holder.progressBar.setIndeterminate(true)
            isSearchActiveConnection = isSearchActive.subscribe {
                holder.itemView.setVisibility(if (it) View.VISIBLE else View.GONE)
            }
        }
        else -> { throw IllegalArgumentException("Unexpected holder type") }
    }.discard()

    override fun getItemCount() = storage.size + 1

    override fun close() {
        sourceConnection.close()
        isSearchActiveConnection.close()
    }

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

    private companion object {
        enum class ViewType(val code: Int) {
            Item(0),
            Progress(1)
        }

        val LOG = loggerFor<SearchModel>()
    }
}
