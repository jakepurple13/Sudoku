package com.programmersbox.sudoku

import dev.teogor.sudoklify.components.Difficulty
import kotlinx.coroutines.flow.Flow

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect class Settings(producePath: () -> String) {
    fun difficultyFlow(): Flow<Difficulty>
    fun gameBoardFlow(): Flow<String?>
    suspend fun updateDifficulty(difficulty: Difficulty)
    suspend fun updateGameBoard(gameBoard: String)
}
