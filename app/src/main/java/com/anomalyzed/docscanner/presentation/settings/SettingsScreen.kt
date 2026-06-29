package com.anomalyzed.docscanner.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.anomalyzed.docscanner.BuildConfig
import com.anomalyzed.docscanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onViewChangelog: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguageCode = remember { 
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "" 
    }
    
    val currentLanguageLabel = when (currentLanguageCode) {
        "en" -> stringResource(R.string.settings_language_english)
        "it" -> stringResource(R.string.settings_language_italian)
        else -> stringResource(R.string.settings_language_system)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Language section
            SettingsSectionHeader(stringResource(R.string.settings_section_language))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_section_language)) },
                supportingContent = { Text(currentLanguageLabel) },
                leadingContent = {
                    Icon(Icons.Filled.Language, contentDescription = null)
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )

            HorizontalDivider()

            // Updates section
            SettingsSectionHeader(stringResource(R.string.settings_section_updates))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_btn_check_updates)) },
                leadingContent = {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                },
                modifier = Modifier.clickable { onCheckForUpdates() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_btn_view_changelog)) },
                leadingContent = {
                    Icon(Icons.Filled.List, contentDescription = null)
                },
                modifier = Modifier.clickable { onViewChangelog() }
            )

            HorizontalDivider()

            // About section
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = {
                    Icon(Icons.Filled.Info, contentDescription = null)
                }
            )
        }
    }

    if (showLanguageDialog) {
        val languages = listOf(
            "" to stringResource(R.string.settings_language_system),
            "en" to stringResource(R.string.settings_language_english),
            "it" to stringResource(R.string.settings_language_italian)
        )

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_section_language)) },
            text = {
                Column {
                    languages.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val localeList = if (code.isEmpty()) {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(code)
                                    }
                                    AppCompatDelegate.setApplicationLocales(localeList)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguageCode == code,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
