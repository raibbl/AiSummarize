package models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.aisummarize.data.db.AppDatabase
import com.example.aisummarize.data.db.SummaryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).summaryDao()
    private val _searchQuery = MutableStateFlow("")
    @OptIn(ExperimentalCoroutinesApi::class)
    val summaries: LiveData<List<SummaryItem>> = _searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) dao.getAllSummaries().asFlow()
        else dao.searchSummaries(query).asFlow()
    }.asLiveData()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }


    fun deleteById(id: Int) {
        viewModelScope.launch {
            dao.deleteSummaryById(id)
        }
    }
}