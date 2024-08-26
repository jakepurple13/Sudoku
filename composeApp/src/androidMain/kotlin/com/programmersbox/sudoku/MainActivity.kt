package com.programmersbox.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                settings = remember { Settings { filesDir.resolve(Settings.DATA_STORE_FILE_NAME).absolutePath } }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}