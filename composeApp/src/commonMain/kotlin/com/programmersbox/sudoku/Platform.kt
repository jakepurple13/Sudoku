package com.programmersbox.sudoku

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform