package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.Menu
import android.view.ViewGroup
import android.view.View
import com.example.subsinthe.crossline.util.ObservableArrayList
import com.example.subsinthe.crossline.util.TokenPool
import com.example.subsinthe.crossline.util.Token
import com.example.subsinthe.crossline.streaming.MusicTrack

class MainLayout() : Fragment() {
    private lateinit var _view: View
    private val tokens = TokenPool()

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.main_layout, parent, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _view = view

        val mainActivity = (getActivity() as MainActivity)
        val toolbar = _view.findViewById<Toolbar>(R.id.toolbar)

        mainActivity.setSupportActionBar(toolbar)
        mainActivity.supportActionBar!!.apply {
            setDisplayShowTitleEnabled(false)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_dehaze)
        }
        setHasOptionsMenu(true)

        val toggle = ActionBarDrawerToggle(
            mainActivity,
            mainActivity.drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mainActivity.drawerLayout.setDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)

        val mainActivity = (getActivity() as MainActivity)
        val layoutManager = LinearLayoutManager(mainActivity)
        val searchResults = ObservableArrayList<MusicTrack>()
        val searchMoreListener = LoadMoreScrollListener(layoutManager)
        val searchQueryListener = SearchQueryListener(
            mainActivity.streamingService,
            mainActivity.uiScope,
            searchResults,
            searchMoreListener.channel,
            loadBatchSize = 10,
            searchDelayOnQueryChange = 1500
        )
        val searchModel = SearchModel(searchResults, searchQueryListener.isSearchActive)

        val searchResultsView = _view.findViewById<RecyclerView>(R.id.search_results)
        val searchView = menu.findItem(R.id.action_search).getActionView() as SearchView

        tokens += Token(searchModel)
        tokens += Token(searchQueryListener)

        searchResultsView.layoutManager = layoutManager
        searchResultsView.adapter = searchModel
        searchResultsView.addOnScrollListener(searchMoreListener)

        searchView.setOnQueryTextListener(searchQueryListener)
    }

    override fun onDetach() {
        super.onDetach()
        tokens.close()
    }
}
