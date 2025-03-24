package com.wissotsky.screentranslator

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wissotsky.screentranslator.ui.theme.ScreenTranslatorTheme
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.wissotsky.screentranslator.di.AppContainer
import com.wissotsky.screentranslator.ui.screens.MainScreen
import com.wissotsky.screentranslator.ui.viewmodels.MainViewModel

private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
private const val TAG = "PermissionScreen"

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize dependency container
        appContainer = (application as ScreenTranslatorApplication).appContainer
        viewModel = MainViewModel(
            appContainer.permissionsManager,
            appContainer.translationModelRepository,
            appContainer.preferencesRepository
        )

        setContent {
            ScreenTranslatorTheme {
                // Remove the .verticalScroll() from Scaffold
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    //add the vertical scroll to the Column inside the Scaffold
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MainScreen(
                            modifier = Modifier, // remove the padding to the main screen, as it is already in the Column above
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}


fun isAccessibilityServiceInstalled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getInstalledAccessibilityServiceList()
    val myServiceComponentName = ComponentName(context, ScreenTranslatorService::class.java)
    Log.d(TAG, "Checking accessibility service: ${myServiceComponentName.flattenToString()}")
    for (enabledService in enabledServices) {
        // Get the ComponentName for the enabled service
        val enabledServiceComponentName = ComponentName(enabledService.resolveInfo.serviceInfo.packageName, enabledService.resolveInfo.serviceInfo.name)
        Log.d(TAG, "Enabled Accessibility Service ID: ${enabledServiceComponentName.flattenToString()}")
        // Compare the component names using flattenToString()
        if (enabledServiceComponentName == myServiceComponentName) {
            Log.d(TAG, "service id: ${myServiceComponentName.flattenToString()} is enabled")
            return true
        }
    }
    return false
}


fun checkOverlayPermission(activity: ComponentActivity, context: Context, requestOverlayPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        // Request overlay permission
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        requestOverlayPermissionLauncher.launch(intent)
    }
}

fun MainActivity.requestOverlayPermission(requestCode: Int = OVERLAY_PERMISSION_REQUEST_CODE) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
    ).apply {
        // No need for "package" in extras, as the framework already knows this from the caller context
    }
    startActivityForResult(intent, requestCode)
}

@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    modelManager: RemoteModelManager
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // State variables for accessibility and overlay permissions
    var isAccessibilityInstalled by remember {
        mutableStateOf(
            isAccessibilityServiceInstalled(
                context
            )
        )
    }
    var isOverlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Launchers for permission requests
    val requestOverlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        Log.d(TAG, "Overlay permission result: ${result.resultCode}")
        // Check if the result code is OK, even if not directly passed in the result
        isOverlayPermissionGranted = Settings.canDrawOverlays(context)
    }

    // Update permission state when the screen recomposes (e.g., after returning from settings)
    LaunchedEffect(Unit) {
        isAccessibilityInstalled = isAccessibilityServiceInstalled(
            context
        )
        isOverlayPermissionGranted = Settings.canDrawOverlays(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Accessibility Service Section
        if (!isAccessibilityInstalled) {
            Button(onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }) {
                Text("Enable Accessibility Service")
            }
            Spacer(modifier = Modifier.padding(8.dp))
        } else {
            Text(text = "Accessibility Service is enabled")
        }

        // Overlay Permission Section
        Spacer(modifier = Modifier.padding(8.dp))
        if (isOverlayPermissionGranted) {
            Text("Screen Overlay Permission Granted")
        } else {
            Button(onClick = {
                if (activity != null) {
                    if (!Settings.canDrawOverlays(context)) {
                        requestOverlayPermissionLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                // No need for "package" in extras, as the framework already knows this from the caller context
                            )
                        )
                    }
                }
            }) {
                Text(text = "Allow screen overlays")
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))
        ModelManagerScreen(modelManager = modelManager)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    modifier: Modifier = Modifier,
    modelManager: com.google.mlkit.common.model.RemoteModelManager
) {
    var downloadedModels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedLanguageCode by remember { mutableStateOf("he") }
    val availableLanguages = TranslateLanguage.getAllLanguages()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedModelsToDelete = remember { mutableStateListOf<String>() }
    var newLanguageCode by remember { mutableStateOf(TextFieldValue("")) }

    fun refreshDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).addOnSuccessListener { models: Set<TranslateRemoteModel> ->
            downloadedModels = models.map { it.getLanguage() }.toSet()
        }
    }

    refreshDownloadedModels()

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Downloaded Models:")
        LazyColumn {
            items(downloadedModels.toList()) { modelCode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
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

        Button(
            onClick = {
                selectedModelsToDelete.forEach { modelCode ->
                    modelManager.deleteDownloadedModel(TranslateRemoteModel.Builder(modelCode).build())
                        .addOnSuccessListener {
                            Log.d("MLKIT", "Model $modelCode deleted")
                            refreshDownloadedModels()
                            selectedModelsToDelete.remove(modelCode)

                        }
                        .addOnFailureListener {
                            Log.e("MLKIT", "Error deleting model $modelCode", it)
                        }
                }
            },
            enabled = selectedModelsToDelete.isNotEmpty()
        ) {
            Text("Delete Selected Models")
        }
        Spacer(modifier = Modifier.padding(8.dp))

        Text(text = "Add New Model")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { isDropdownExpanded = true }) {
                Text(text = "Select Language")
            }
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
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = selectedLanguageCode)
        }
        Button(onClick = {
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(TranslateRemoteModel.Builder(selectedLanguageCode).build(), conditions)
                .addOnSuccessListener {
                    Log.d("MLKIT", "Model $selectedLanguageCode downloaded")
                    refreshDownloadedModels()
                }
                .addOnFailureListener {
                    Log.e("MLKIT", "Error downloading model $selectedLanguageCode", it)
                }
        }) {
            Text(text = "Download Language")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ScreenTranslatorTheme {
        val previewModelManager = RemoteModelManager.getInstance()
        ModelManagerScreen(modelManager = previewModelManager)
    }
}