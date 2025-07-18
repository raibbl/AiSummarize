package models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.aisummarize.data.db.AppDatabase
import com.example.aisummarize.data.db.SummaryItem

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).summaryDao()
    val summaries: LiveData<List<SummaryItem>> = dao.getAllSummaries()
}