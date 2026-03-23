package models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.data.db.SummaryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).summaryDao()
    private val _searchQuery = MutableStateFlow("")
    private val _activeFilters = MutableStateFlow<Set<String>>(emptySet())
    val activeFilters: MutableStateFlow<Set<String>> get() = _activeFilters

    @OptIn(ExperimentalCoroutinesApi::class)
    val summaries: LiveData<List<SummaryItem>> = _searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) dao.getAllSummaries().asFlow()
        else dao.searchSummaries(query).asFlow()
    }.combine(_activeFilters) { items, filters ->
        if (filters.isEmpty()) items
        else items.filter { item ->
            val itemTags = item.tagList().map { it.lowercase() }.toSet()
            filters.all { f -> f.lowercase() in itemTags }
        }
    }.asLiveData()

    /** All distinct tags across every summary, flattened and de-duped. */
    val allTags: LiveData<List<String>> = dao.getAllRawTags().map { rawList ->
        rawList.flatMap { raw ->
            raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilter(tag: String) {
        _activeFilters.value = _activeFilters.value.let {
            if (tag in it) it - tag else it + tag
        }
    }

    fun updateTags(id: Int, tags: List<String>) {
        viewModelScope.launch {
            val joined = tags.joinToString(",").ifEmpty { null }
            dao.updateTags(id, joined)
        }
    }

    fun deleteById(id: Int) {
        viewModelScope.launch {
            dao.deleteSummaryById(id)
        }
    }

    fun insertOnboardingIfEmpty() {
        viewModelScope.launch {
            if (dao.getSummaryCount() == 0) {
                dao.insertSummary(
                    SummaryItem(
                        type = "Info",
                        link = null,
                        title = "How to use Briefly",
                        summary = "To get started, open an article in your browser, tap Share, and choose 'Summarize'. You can also add the Briefly's Quick Settings tile to capture your screen and summarize on-screen text.",
                        imagePath = null
                    )
                )
            }
        }
    }
}
