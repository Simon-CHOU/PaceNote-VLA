package com.pacenote.vla.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacenote.vla.settings.SettingsViewModel

/**
 * Settings screen with LHD/RHD toggle and language selection
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isLhd by viewModel.isLhd.collectAsState()
    val language by viewModel.language.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Settings
            SettingsCard(title = "Vehicle Settings") {
                LhdRhdToggle(
                    isLhd = isLhd,
                    onToggle = { viewModel.toggleLhdRhd() }
                )
            }

            // Language Settings
            SettingsCard(title = "Language / 语言") {
                LanguageSelector(
                    currentLanguage = language,
                    onLanguageSelected = { viewModel.setLanguage(it) }
                )
            }

            // Audio & Haptics
            SettingsCard(title = "Audio & Haptics") {
                SwitchSetting(
                    label = "Voice Output",
                    checked = voiceEnabled,
                    onCheckedChange = { viewModel.toggleVoice() }
                )
                SwitchSetting(
                    label = "Haptic Feedback",
                    checked = hapticEnabled,
                    onCheckedChange = { viewModel.toggleHaptic() }
                )
            }

            // About
            SettingsCard(title = "About") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PaceNote VLA",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your AI Co-driver\nInspired by WRC Navigators",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun LhdRhdToggle(
    isLhd: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Steering Wheel Position",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (isLhd) "Left Hand Drive (LHD)" else "Right Hand Drive (RHD)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = isLhd,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun LanguageSelector(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "App Language",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageButton(
                text = "English",
                isSelected = currentLanguage == "en",
                onClick = { onLanguageSelected("en") },
                modifier = Modifier.weight(1f)
            )
            LanguageButton(
                text = "中文",
                isSelected = currentLanguage == "zh",
                onClick = { onLanguageSelected("zh") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LanguageButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = if (isSelected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Text(text)
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
