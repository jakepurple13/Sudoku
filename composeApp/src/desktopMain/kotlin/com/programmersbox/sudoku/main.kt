package com.programmersbox.sudoku

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.teogor.sudoklify.mapToSudokuString
import dev.teogor.sudoklify.puzzle.generateGridWithGivens

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sudoku",
    ) {
        App()
    }
}

fun main1() {
    val d = SudokuHandler()
    val p = d.puzzle
    p.generateGridWithGivens().forEach {
        it.forEach {
            print("$it ")
        }
        println()
    }

    println(p.generateGridWithGivens().mapToSudokuString())
}