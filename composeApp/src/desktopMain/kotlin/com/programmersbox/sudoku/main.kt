package com.programmersbox.sudoku

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.teogor.sudoklify.mapToSudokuString
import dev.teogor.sudoklify.puzzle.generateGridWithGivens

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sudoku",
    ) {
        App(
            settings = remember { Settings { Settings.DATA_STORE_FILE_NAME } }
        )
    }
}

fun main1() {
    val d = SudokuHandler(Settings { Settings.DATA_STORE_FILE_NAME })
    val p = d.puzzle
    p.generateGridWithGivens().forEach {
        it.forEach {
            print("$it ")
        }
        println()
    }

    println(p.generateGridWithGivens().mapToSudokuString())
}