package com.programmersbox.sudoku

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.teogor.sudoklify.components.Difficulty
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toPath

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

private lateinit var dataStore: DataStore<Preferences>

actual class Settings actual constructor(
    producePath: () -> String,
) {
    init {
        if (!::dataStore.isInitialized)
            dataStore = PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
    }

    companion object {
        const val DATA_STORE_FILE_NAME = "sudoku.preferences_pb"
        val DIFFICULTY_KEY = stringPreferencesKey("mode_difficulty")
        val GAME_BOARD_KEY = stringPreferencesKey("game_board")
    }

    actual fun difficultyFlow() = dataStore.data
        .mapNotNull { it[DIFFICULTY_KEY] }
        .mapNotNull { runCatching { Difficulty.valueOf(it) }.getOrNull() ?: Difficulty.EASY }

    actual fun gameBoardFlow() = dataStore.data.map { it[GAME_BOARD_KEY] }

    actual suspend fun updateDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[DIFFICULTY_KEY] = difficulty.name }
    }

    actual suspend fun updateGameBoard(gameBoard: String) {
        dataStore.edit { it[GAME_BOARD_KEY] = gameBoard }
    }
}