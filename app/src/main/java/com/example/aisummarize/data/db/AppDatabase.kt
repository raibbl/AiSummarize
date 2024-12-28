import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aisummarize.data.db.SummaryItem
import com.example.aisummarize.data.db.SummaryDao

@Database(entities = [SummaryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summaryDao(): SummaryDao
}
