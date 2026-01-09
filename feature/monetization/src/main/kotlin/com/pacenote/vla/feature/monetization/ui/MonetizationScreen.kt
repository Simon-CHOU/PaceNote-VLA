package com.pacenote.vla.feature.monetization.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacenote.vla.feature.monetization.viewmodel.MonetizationViewModel

/**
 * Monetization screen showing subscription options
 */
@Composable
fun MonetizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonetizationViewModel = hiltViewModel()
) {
    val isPro by viewModel.isProFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPro) {
                ProStatusCard()
            } else {
                SubscriptionOptions(
                    onMonthlySubscribe = { viewModel.purchaseSubscription(android.app.Activity()) },
                    onYearlySubscribe = { viewModel.purchaseSubscription(android.app.Activity()) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TrialInfo()
        }
    }
}

@Composable
private fun ProStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "★ Pro Active",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("You have full access to all PaceNote VLA features")
        }
    }
}

@Composable
private fun SubscriptionOptions(
    onMonthlySubscribe: () -> Unit,
    onYearlySubscribe: () -> Unit
) {
    Text(
        text = "Choose Your Plan",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Yearly plan (recommended)
    SubscriptionCard(
        title = "Yearly",
        price = "$39.99/year",
        features = listOf(
            "Save 40% compared to monthly",
            "Full access to all features",
            "Priority support",
            "Early access to new features"
        ),
        isRecommended = true,
        onSubscribe = onYearlySubscribe
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Monthly plan
    SubscriptionCard(
        title = "Monthly",
        price = "$4.99/month",
        features = listOf(
            "Full access to all features",
            "Cancel anytime"
        ),
        isRecommended = false,
        onSubscribe = onMonthlySubscribe
    )
}

@Composable
private fun SubscriptionCard(
    title: String,
    price: String,
    features: List<String>,
    isRecommended: Boolean,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 8.dp else 2.dp
        ),
        colors = if (isRecommended) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isRecommended) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "RECOMMENDED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = price,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            features.forEach { feature ->
                Text(
                    text = "✓ $feature",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subscribe")
            }
        }
    }
}

@Composable
private fun TrialInfo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "About Free Trial",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "New users get a 7-day free trial with full access. After the trial, continue with ads or upgrade to Pro for an ad-free experience.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
