package com.tigerpaw.launcher.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.apps.AppRepository
import com.tigerpaw.launcher.core.data.search.SearchRepository
import com.tigerpaw.launcher.core.data.search.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GroupedResults(
    val suggestions: List<SearchResult.Suggestion> = emptyList(),
    val apps: List<SearchResult.App> = emptyList(),
    val actions: List<SearchResult.AppAction> = emptyList(),
    val files: List<SearchResult.File> = emptyList(),
) {
    val isEmpty get() = apps.isEmpty() && actions.isEmpty() && files.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class InlineSearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<GroupedResults> = _query
        .debounce(120)
        .flatMapLatest { q ->
            if (q.isBlank()) {
                searchRepository.suggestions().map { suggs ->
                    GroupedResults(suggestions = suggs)
                }
            } else {
                searchRepository.search(q).map { list ->
                    GroupedResults(
                        apps = list.filterIsInstance<SearchResult.App>(),
                        actions = list.filterIsInstance<SearchResult.AppAction>(),
                        files = list.filterIsInstance<SearchResult.File>(),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupedResults())

    fun onQueryChange(q: String) { _query.value = q }

    fun clearQuery() { _query.value = "" }

    fun launch(context: Context, result: SearchResult) {
        when (result) {
            is SearchResult.App -> appRepository.launch(context, result.app)
            is SearchResult.Suggestion -> appRepository.launch(context, result.app)
            is SearchResult.File -> openFile(context, result)
            is SearchResult.AppAction -> appRepository.launchAction(context, result)
        }
    }

    private fun openFile(context: Context, file: SearchResult.File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}
