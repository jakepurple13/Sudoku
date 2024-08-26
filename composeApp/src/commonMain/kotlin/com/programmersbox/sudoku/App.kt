package com.programmersbox.sudoku

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.DynamicMaterialTheme
import dev.teogor.sudoklify.common.model.SudokuPuzzle
import dev.teogor.sudoklify.common.types.Difficulty
import dev.teogor.sudoklify.common.types.SudokuType
import dev.teogor.sudoklify.core.generation.createPuzzle
import dev.teogor.sudoklify.core.generation.difficulty
import dev.teogor.sudoklify.core.generation.seed
import dev.teogor.sudoklify.core.generation.seeds
import dev.teogor.sudoklify.core.generation.sudokuParamsBuilder
import dev.teogor.sudoklify.core.generation.sudokuType
import dev.teogor.sudoklify.ktx.generateGridWithGivens
import dev.teogor.sudoklify.ktx.toSeed
import dev.teogor.sudoklify.seeds.combinedSeeds
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.collections.forEach
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(
    sudokuHandler: SudokuHandler = viewModel { SudokuHandler() },
) {
    val windowInfo = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowInfo) {
        if (windowInfo) sudokuHandler.resumeTimer()
        else sudokuHandler.pauseTimer()
    }

    DynamicMaterialTheme(
        seedColor = Color(0xff90CAF9)
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val px = with(LocalDensity.current) { CardDefaults.outlinedCardBorder().width.toPx() }

        var showDialog by remember(sudokuHandler.hasWon) { mutableStateOf(sudokuHandler.hasWon) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Are you sure?") },
                text = { Text("You will lose all your progress.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            sudokuHandler.generateGrid()
                        }
                    ) {
                        Text("New Game")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Not yet")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Sudoku") },
                    actions = { Text(sudokuHandler.timeText) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (sudokuHandler.hasWon) sudokuHandler.generateGrid()
                                else showDialog = true
                            }
                        ) { Icon(Icons.Default.Add, null) }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    actions = {},
                    floatingActionButton = {
                        NumberHighlighter(
                            chosenDigit = sudokuHandler.highlightedDigit,
                            digits = (1..sudokuHandler.sudokuSpec.sudokuType.uniqueDigitsCount).toList(),
                            onValueChange = { sudokuHandler.highlightedDigit = it }
                        )
                    }
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(sudokuHandler.size + 1),
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                sudokuHandler.generatedGrid.forEachIndexed { columnIndex, row ->
                    row.forEachIndexed { rowIndex, number ->
                        item {
                            Digit(
                                value = number.number,
                                digits = (1..sudokuHandler.sudokuSpec.sudokuType.uniqueDigitsCount).toList(),
                                highlight = { number.number == sudokuHandler.highlightedDigit && number.number != 0 },
                                shape = RoundedCornerShape(
                                    topStart = when {
                                        columnIndex == 0 && rowIndex == 0 -> 24.dp
                                        else -> 0.dp
                                    },
                                    topEnd = when {
                                        columnIndex == 0 && rowIndex == sudokuHandler.size -> 24.dp
                                        else -> 0.dp
                                    },
                                    bottomEnd = when {
                                        columnIndex == sudokuHandler.size && rowIndex == sudokuHandler.size -> 24.dp
                                        else -> 0.dp
                                    },
                                    bottomStart = when {
                                        columnIndex == sudokuHandler.size && rowIndex == 0 -> 24.dp
                                        else -> 0.dp
                                    }
                                ),
                                canModify = !number.given,
                                onValueChange = { sudokuHandler.updateGrid(it, rowIndex, columnIndex) },
                                modifier = Modifier
                                    .drawWithContent {
                                        drawContent()
                                        drawPath(
                                            drawShape(
                                                px,
                                                showBottomLine = false,
                                                showTopLine = columnIndex % 3 == 0 && columnIndex != 0,
                                                showLeftLine = rowIndex % 3 == 0 && rowIndex != 0,
                                                showRightLine = false,
                                            ),
                                            primary
                                        )
                                    }
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Digit(
    value: Int,
    digits: List<Int>,
    onValueChange: (Int) -> Unit,
    shape: Shape,
    canModify: Boolean,
    highlight: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    var showDropDown by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = { if (canModify) showDropDown = true },
        shape = shape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = animateColorAsState(
                if (highlight()) MaterialTheme.colorScheme.secondary
                else Color.Unspecified
            ).value
        ),
        modifier = modifier
    ) {
        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = { showDropDown = false },
        ) {
            DropdownMenuItem(
                text = { Text("Clear") },
                onClick = {
                    onValueChange(0)
                    showDropDown = false
                },
            )
            digits.forEach { grade ->
                DropdownMenuItem(
                    text = { Text(grade.toString()) },
                    onClick = {
                        onValueChange(grade)
                        showDropDown = false
                    },
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .sizeIn(48.dp, 48.dp),
        ) {
            Text(
                value.takeIf { it != 0 }?.toString() ?: "",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun NumberHighlighter(
    chosenDigit: Int,
    digits: List<Int>,
    onValueChange: (Int) -> Unit,
) {
    var showDropDown by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = { showDropDown = false },
    ) {
        DropdownMenuItem(
            text = { Text("Clear") },
            onClick = {
                onValueChange(0)
                showDropDown = false
            },
        )
        digits.forEach { grade ->
            DropdownMenuItem(
                text = { Text(grade.toString()) },
                onClick = {
                    onValueChange(grade)
                    showDropDown = false
                },
            )
        }
    }

    ExtendedFloatingActionButton(
        onClick = { showDropDown = !showDropDown },
        text = { Text(chosenDigit.toString()) },
        icon = { Text("Highlight") }
    )
}

class SudokuHandler : ViewModel() {
    var sudokuSpec = sudokuParamsBuilder {
        sudokuType { SudokuType.Sudoku9x9 }
        difficulty { Difficulty.EASY }
        seed { Random.nextLong(0, Long.MAX_VALUE).toSeed() }
        seeds { combinedSeeds }
    }

    var highlightedDigit by mutableIntStateOf(0)

    var hasWon by mutableStateOf(false)
    private val stopwatch = Stopwatch(tick = 1L)
    private var time by mutableLongStateOf(0)
    private val minutes by derivedStateOf { time.milliseconds.inWholeMinutes }
    private val seconds by derivedStateOf { (time - minutes * 60 * 1000).milliseconds.inWholeSeconds }
    val timeText by derivedStateOf {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    init {
        snapshotFlow { hasWon }
            .onEach {
                if (it) stopwatch.pause()
            }
            .launchIn(viewModelScope)

        stopwatch.time
            .onEach { time++ }
            .launchIn(viewModelScope)
    }

    var puzzle: SudokuPuzzle = sudokuSpec.createPuzzle()

    val generatedGrid: SnapshotStateList<SnapshotStateList<SudokuDigit>> = mutableStateListOf()

    var size = sudokuSpec.sudokuType.gridSize.height * sudokuSpec.sudokuType.gridSize.width - 1

    init {
        generateGrid()
    }

    fun generateGrid(
        difficulty: Difficulty = Difficulty.EASY,
    ) {
        time = 0
        hasWon = false
        sudokuSpec = sudokuParamsBuilder {
            sudokuType { SudokuType.Sudoku9x9 }
            difficulty { difficulty }
            seed { Random.nextLong(0, Long.MAX_VALUE).toSeed() }
            seeds { combinedSeeds }
        }
        size = sudokuSpec.sudokuType.gridSize.height * sudokuSpec.sudokuType.gridSize.width - 1
        puzzle = sudokuSpec.createPuzzle()
        generatedGrid.clear()
        generatedGrid.addAll(
            sudokuSpec
                .createPuzzle()
                .generateGridWithGivens()
                .map {
                    it
                        .map { SudokuDigit(it, it != 0) }
                        .toMutableStateList()
                }
                .toMutableStateList()
        )

        puzzle.solution.forEach {
            it.forEach {
                print("$it ")
            }
            println()
        }
    }

    fun updateGrid(number: Int, row: Int, column: Int) {
        generatedGrid[column][row] = SudokuDigit(number)

        for (c in generatedGrid.indices) {
            for (r in generatedGrid[c].indices) {
                if (generatedGrid[c][r].number != puzzle.solution[c][r]) {
                    return
                }
            }
        }
        hasWon = true
    }

    fun pauseTimer() {
        stopwatch.pause()
    }

    fun resumeTimer() {
        if (!hasWon)
            stopwatch.start()
    }
}

data class SudokuDigit(
    val number: Int,
    val given: Boolean = false,
)

fun ContentDrawScope.drawShape(
    lineThicknessPx: Float,
    showBottomLine: Boolean = true,
    showTopLine: Boolean = true,
    showLeftLine: Boolean = true,
    showRightLine: Boolean = true,
): Path = Path().apply {
    if (showBottomLine) {
        // 1) Bottom-left corner
        moveTo(0f, size.height)
        // 2) Bottom-right corner
        lineTo(size.width, size.height)
        // 3) Top-right corner
        lineTo(size.width, size.height - lineThicknessPx)
        // 4) Top-left corner
        lineTo(0f, size.height - lineThicknessPx)
    }
    if (showLeftLine) {
        moveTo(0f, 0f)
        lineTo(0f, size.height)
        lineTo(lineThicknessPx, size.height)
        lineTo(lineThicknessPx, 0f)
    }
    if (showTopLine) {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, lineThicknessPx)
        lineTo(0f, lineThicknessPx)
    }
    if (showRightLine) {
        moveTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(size.width - lineThicknessPx, size.height)
        lineTo(size.width - lineThicknessPx, 0f)
    }
}
