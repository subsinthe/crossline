package com.example.subsinthe.crossline

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.Button
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.ServiceType as StreamingServiceType
import com.example.subsinthe.crossline.util.IObservableMap
import com.example.subsinthe.crossline.util.MappingOp
import com.example.subsinthe.crossline.util.loggerFor
import kotlin.collections.LinkedHashSet
import kotlin.collections.Set
import java.io.Closeable

class StreamingSettingsModel(
    source: IObservableMap<StreamingServiceType, IStreamingService>,
    private val ignoredServices: Set<StreamingServiceType>
) : RecyclerView.Adapter<StreamingSettingsModel.ItemHolder>(), Closeable {
    private val storage = LinkedHashSet<StreamingServiceType>()
    private val sourceConnection = source.subscribe { onSourceChanged(it) }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val enterButton = view.findViewById<Button>(R.id.enter_settings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.streaming_settings_item, parent, false
        ) as View
    )

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val type = storage.elementAt(position)
        holder.enterButton.text = when (type) {
            StreamingServiceType.Filesystem -> { "Filesystem" }
            else -> { throw AssertionError("Unexpected service type: $type") }
        }
    }

    override fun getItemCount() = storage.size

    override fun close() = sourceConnection.close()

    private fun onSourceChanged(op: MappingOp<StreamingServiceType, IStreamingService>) {
        LOG.fine("onSourceChanged($op)")

        if (op.key in ignoredServices) {
            LOG.fine("Skipping ${op.key} as ignored")
            return
        }

        when (op) {
            is MappingOp.Added -> {
                if (!storage.add(op.key))
                    throw IllegalArgumentException("${op.key} is already in storage")
                notifyItemInserted(storage.size - 1)
            }
            is MappingOp.Removed -> {
                val index = storage.indexOf(op.key)
                if (index < 0)
                    IllegalArgumentException("${op.key} is not present in storage")

                storage.remove(op.key)
                notifyItemRemoved(index)
            }
            else -> {}
        }
    }

    private companion object { val LOG = loggerFor<StreamingSettingsModel>() }
}
