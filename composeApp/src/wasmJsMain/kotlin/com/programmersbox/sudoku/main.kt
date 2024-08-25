package com.programmersbox.sudoku

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        // TODO viewModel() is not yet fully supported and does not work properly
        //  in wasmJsBrowser.
        App(
            sudokuHandler = remember { SudokuHandler() },
        )
    }
}