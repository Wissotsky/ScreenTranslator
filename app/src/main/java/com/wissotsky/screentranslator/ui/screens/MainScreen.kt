package com.wissotsky.screentranslator.ui.screens

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.wissotsky.screentranslator.ui.viewmodels.MainViewModel

private const val TAG = "MainScreen"

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    
    // Update permission state when the screen recomposes
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Screen Translator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Permissions Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Required Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Accessibility Service Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isAccessibilityServiceInstalled) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Installed",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accessibility Service is installed")
                    } else {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Text("Install Accessibility Service")
                        }
                    }
                }

                // Floating Button Reminder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("(Make sure to enable accessibility shortcut)")
                }

                
                // Overlay Permission Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isOverlayPermissionGranted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Enabled",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Screen Overlay Permission Granted")
                    } else {
                        val requestOverlayPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) {
                            viewModel.refreshPermissions()
                        }
                        
                        Button(onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            requestOverlayPermissionLauncher.launch(intent)
                        }) {
                            Text(text = "Allow Screen Overlays")
                        }
                    }
                }
            }
        }
        
        // Translation Models Section
        TranslationModelsSection(viewModel = viewModel)
        
        // Language Configuration Section
        LanguageConfigurationSection(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationModelsSection(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    var selectedLanguageCode by remember { mutableStateOf("he") }
    val availableLanguages = TranslateLanguage.getAllLanguages()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val selectedModelsToDelete = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedModels()
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Translation Models",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Downloaded Models:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            if (viewModel.downloadedModels.isEmpty()) {
                Text(
                    text = "No models downloaded yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(viewModel.downloadedModels.toList()) { modelCode -> 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedModelsToDelete.contains(modelCode),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedModelsToDelete.add(modelCode)
                                    } else {
                                        selectedModelsToDelete.remove(modelCode)
                                    }
                                }
                            )
                            Text(text = modelCode)
                        }
                    }
                }
            }
            
            Button(
                onClick = {
                    selectedModelsToDelete.forEach { modelCode ->
                        viewModel.deleteModel(
                            modelCode,
                            onSuccess = {
                                Log.d(TAG, "Model $modelCode deleted")
                                selectedModelsToDelete.remove(modelCode)
                                viewModel.refreshDownloadedModels()
                            },
                            onFailure = {
                                Log.e(TAG, "Error deleting model $modelCode", it)
                            }
                        )
                    }
                },
                enabled = selectedModelsToDelete.isNotEmpty(),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Delete Selected Models")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Add New Model",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { isDropdownExpanded = true }) {
                    Text(text = "Select Language")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = selectedLanguageCode)
                
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    availableLanguages.forEach { languageCode ->
                        DropdownMenuItem(
                            text = { Text(text = languageCode) },
                            onClick = {
                                selectedLanguageCode = languageCode
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            
            Button(
                onClick = {
                    viewModel.downloadModel(
                        selectedLanguageCode,
                        onSuccess = {
                            Log.d(TAG, "Model $selectedLanguageCode downloaded")
                            viewModel.refreshDownloadedModels()
                        },
                        onFailure = {
                            Log.e(TAG, "Error downloading model $selectedLanguageCode", it)
                        }
                    )
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(text = "Download Language")
            }
        }
    }
}

@Composable
fun LanguageConfigurationSection(modifier: Modifier = Modifier,viewModel: MainViewModel) {
    val downloadedModels = viewModel.downloadedModels
    var isSourceDropdownExpanded by remember { mutableStateOf(false) }
    var isTargetDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Language Configuration",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Source Language:")
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { isSourceDropdownExpanded = true }) {
                    Text(text = viewModel.sourceLanguage)
                }
                DropdownMenu(
                    expanded = isSourceDropdownExpanded,
                    onDismissRequest = { isSourceDropdownExpanded = false }
                ) {
                    downloadedModels.forEach { languageCode ->
                        DropdownMenuItem(
                            text = { Text(text = languageCode) },
                            onClick = {
                                viewModel.updateSourceLanguage(languageCode)
                                isSourceDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Target Language:")
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { isTargetDropdownExpanded = true }) {
                    Text(text = viewModel.targetLanguage)
                }
                DropdownMenu(
                    expanded = isTargetDropdownExpanded,
                    onDismissRequest = { isTargetDropdownExpanded = false }
                ) {
                    downloadedModels.forEach { languageCode ->
                        DropdownMenuItem(
                            text = { Text(text = languageCode) },
                            onClick = {
                                viewModel.updateTargetLanguage(languageCode)
                                isTargetDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}