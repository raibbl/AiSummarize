package com.raibbl.AiAnalyze

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpgradeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FREE_LIMIT = "extra_free_limit"
        const val EXTRA_RESET_DATE = "extra_reset_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val freeLimit = intent.getIntExtra(EXTRA_FREE_LIMIT, 40)
        val resetDate = intent.getLongExtra(EXTRA_RESET_DATE, 0L)

        setContent {
            AiSummarizeTheme {
                UpgradePromptScreen(
                    freeLimit = freeLimit,
                    resetDate = resetDate,
                    onUpgradeClick = {
                        val app = application.brieflyApp
                        app.billingManager.launchSubscriptionFlow(this)
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradePromptScreen(
    freeLimit: Int,
    resetDate: Long,
    onUpgradeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You've used all $freeLimit free summaries this month",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Upgrade to Briefly Unlimited for unrestricted summaries.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Upgrade to Briefly Unlimited",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\$1.49/mo",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (resetDate > 0L) {
                Spacer(modifier = Modifier.height(16.dp))
                val formatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(resetDate))
                Text(
                    text = "Your free summaries reset on $formatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
