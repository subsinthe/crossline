package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import com.example.subsinthe.crossline.util.Multicast
import com.example.subsinthe.crossline.util.TokenPool
import com.example.subsinthe.crossline.util.Token

class SettingsLayout : Fragment() {
    private val _closed = Multicast<Unit>()
    private val tokens = TokenPool()

    fun closed(handler: (Unit) -> Unit) = _closed.subscribe(handler)

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.settings_layout, parent, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = (getActivity() as MainActivity)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.apply {
            setTitle("Settings")
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) { _closed(Unit) }
            })
        }

        mainActivity.onSettingsLayoutCreated(this)

        val streamingSettingsModel = StreamingSettingsModel(
            mainActivity.streamingSettings.serviceTypes
        )
        val streamingSettingsView = view.findViewById<RecyclerView>(R.id.streaming_settings)

        tokens += Token(streamingSettingsModel)

        streamingSettingsView.layoutManager = LinearLayoutManager(mainActivity)
        streamingSettingsView.adapter = streamingSettingsModel
    }

    override fun onDetach() {
        super.onDetach()
        tokens.close()
    }
}
