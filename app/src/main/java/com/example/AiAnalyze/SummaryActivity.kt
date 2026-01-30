package com.raibbl.AiAnalyze

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.data.db.SummaryItem
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.raibbl.AiAnalyze.ui.components.ReadingSettings
import com.raibbl.AiAnalyze.ui.components.ReadingSettingsSaver
import com.raibbl.AiAnalyze.ui.components.TextSizePreset
import com.raibbl.AiAnalyze.utils.EXTRA_SUMMARY_ID
import androidx.compose.foundation.horizontalScroll

class SummaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val summaryId = intent.getIntExtra(EXTRA_SUMMARY_ID, -1)

        if (summaryId == -1) {
            Toast.makeText(this, "Invalid summary ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@SummaryActivity)
            val summaryItem = db.summaryDao().getSummaryById(summaryId)

            if (summaryItem == null) {
                Toast.makeText(this@SummaryActivity, "Summary not found for $summaryId", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            setContent {
                AiSummarizeTheme {
                    SummaryScreen(summaryItem)
                }
            }
        }
    }
}

@Composable
fun SummaryScreen(summaryItem: SummaryItem) {
    SummaryContent(
        title = summaryItem.title,
        summaryText = summaryItem.summary,
        link = summaryItem.link,
        imagePath = summaryItem.imagePath
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryContent(
    title: String,
    summaryText: String,
    link: String? = null,
    imagePath: String? = null
) {
    val currentContext = LocalContext.current

    // Selected bottom nav tab (-1 = none, 0 = Snippets/Home, 1 = Readability)
    var selectedTab by rememberSaveable { mutableStateOf(-1) }
    
    // Reading settings state
    var readingSettings by rememberSaveable(stateSaver = ReadingSettingsSaver) {
        mutableStateOf(ReadingSettings())
    }
    // Derive text style from settings
    val base = MaterialTheme.typography.bodyLarge
    val baseFont = if (base.fontSize.isUnspecified) 16.sp else base.fontSize
    val scaledFont = (baseFont.value * readingSettings.sizePreset.scale).sp

    val baseLine = if (base.lineHeight.isUnspecified) (baseFont.value * 1.4f).sp else base.lineHeight
    val lineHeight = if (readingSettings.comfortableSpacing)
        (baseLine.value * 1.25f).sp
    else
        baseLine

    val summaryStyle = base.merge(
        TextStyle(
            fontSize = scaledFont,
            lineHeight = lineHeight
        )
    )

    val summaryAlign = if (readingSettings.centerText) TextAlign.Center else TextAlign.Start

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { (currentContext as? ComponentActivity)?.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, buildString {
                                append(title)
                                append("\n\n")
                                append(summaryText)
                                if (!link.isNullOrBlank()) {
                                    append("\n\n")
                                    append("Link: $link")
                                }
                            })
                        }
                        currentContext.startActivity(Intent.createChooser(shareIntent, "Share summary"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        },
        bottomBar = {
            SummaryBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                settings = readingSettings,
                onSettingsChange = { readingSettings = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageBitmap = remember(imagePath) {
                imagePath?.let {
                    val file = File(it)
                    if (file.exists()) BitmapFactory.decodeFile(it)?.asImageBitmap() else null
                }
            }

            if (imageBitmap != null) {
                var showImage by remember { mutableStateOf(false) }

                TextButton(onClick = { showImage = !showImage }) {
                    Text(if (showImage) "Hide screenshot" else "View screenshot")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showImage) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Captured screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Text(
                text = summaryText,
                style = summaryStyle,
                textAlign = summaryAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (!link.isNullOrBlank()) {
                Text(
                    text = "Link:  $link",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                            currentContext.startActivity(intent)
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SummaryBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    settings: ReadingSettings,
    onSettingsChange: (ReadingSettings) -> Unit
) {
    val context = LocalContext.current
    
    Column {
        // Tab content area
        if (selectedTab == 1) {
            // Readability options
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Reading Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Text size presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextSizePreset.entries.forEach { preset ->
                            val selected = settings.sizePreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSettingsChange(settings.copy(sizePreset = preset)) },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Toggle options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spacing",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = settings.comfortableSpacing,
                                onCheckedChange = { onSettingsChange(settings.copy(comfortableSpacing = it)) }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Center",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = settings.centerText,
                                onCheckedChange = { onSettingsChange(settings.copy(centerText = it)) }
                            )
                        }
                    }
                }
            }
        }
        
        // Navigation bar
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Home, contentDescription = "Snippets") },
                label = { Text("Snippets") },
                selected = selectedTab == 0,
                onClick = {
                    onTabSelected(0)
                    // Navigate to home/MainActivity
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.FormatSize, contentDescription = "Readability") },
                label = { Text("Readability") },
                selected = selectedTab == 1,
                onClick = { 
                    // Toggle: if already selected, deselect (go back to 0)
                    onTabSelected(if (selectedTab == 1) 0 else 1)
                }
            )
        }
    }
}
