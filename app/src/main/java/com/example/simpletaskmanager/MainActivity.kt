package com.example.simpletaskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.simpletaskmanager.ui.screens.TaskListScreen
import com.example.simpletaskmanager.ui.theme.SimpleTaskManagerTheme
import com.example.simpletaskmanager.ui.viewmodel.TaskViewModel
import com.example.simpletaskmanager.ui.viewmodel.TaskViewModelFactory

/**
 * Single-activity entry point for the Simple Task Manager app.
 *
 * We deliberately keep this class thin – all business logic lives in
 * [TaskViewModel] and all UI lives in Composable functions.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(
                SimpleTaskApp.database.noteDao(),
                SimpleTaskApp.userPreferences
            )
        )[TaskViewModel::class.java]

        setContent {
            // Observe the user's explicit theme preference from the ViewModel.
            // If null, fall back to the system setting.
            val themeOverride by viewModel.isDarkTheme.collectAsState()
            val systemDark    = isSystemInDarkTheme()
            val useDark       = themeOverride ?: systemDark

            SimpleTaskManagerTheme(darkTheme = useDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    TaskListScreen(
                        viewModel     = viewModel,
                        isSystemDark  = systemDark
                    )
                }
            }
        }
    }
}
