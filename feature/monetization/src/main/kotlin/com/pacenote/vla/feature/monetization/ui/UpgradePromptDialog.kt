package com.pacenote.vla.feature.monetization.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Upgrade to Pro dialog
 */
@Composable
fun UpgradePromptDialog(
    onDismiss: () -> Unit,
    onUpgradeMonthly: () -> Unit,
    onUpgradeYearly: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Upgrade to Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your 7-day trial has ended. Upgrade to continue using PaceNote VLA without ads.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Yearly option (highlighted)
                SubscriptionOption(
                    title = "Yearly",
                    price = "$39.99/year",
                    savings = "Save 40%",
                    isRecommended = true,
                    onClick = onUpgradeYearly
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly option
                SubscriptionOption(
                    title = "Monthly",
                    price = "$4.99/month",
                    savings = null,
                    isRecommended = false,
                    onClick = onUpgradeMonthly
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Maybe later")
                }
            }
        }
    }
}

@Composable
private fun SubscriptionOption(
    title: String,
    price: String,
    savings: String?,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = true
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
                Text(price)
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (savings != null) {
                Text(
                    text = savings,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF6B00)
                )
            }
            if (isRecommended) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "★",
                    color = Color(0xFFFF6B00)
                )
            }
        }
    }
}

/**
 * Trial expired dialog
 */
@Composable
fun TrialExpiredDialog(
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Trial Expired") },
        text = {
            Text("Your 7-day free trial has ended. Choose an option to continue:")
        },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Text("View Plans")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Continue with Ads")
            }
        }
    )
}
