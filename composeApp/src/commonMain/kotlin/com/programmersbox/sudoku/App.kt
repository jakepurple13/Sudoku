package com.programmersbox.sudoku

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import dev.teogor.sudoklify.ExperimentalSudoklifyApi
import dev.teogor.sudoklify.SudoklifyArchitect
import dev.teogor.sudoklify.components.Difficulty
import dev.teogor.sudoklify.components.Dimension
import dev.teogor.sudoklify.components.Seed
import dev.teogor.sudoklify.presets.loadPresetSchemas
import dev.teogor.sudoklify.puzzle.SudokuPuzzle
import dev.teogor.sudoklify.puzzle.SudokuSpec
import dev.teogor.sudoklify.puzzle.generateGridWithGivens
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalSudoklifyApi::class, ExperimentalMaterial3Api::class)
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
                title = { Text(if (sudokuHandler.hasWon) "You Win!" else "Are you sure?") },
                text = {
                    Text(
                        if (sudokuHandler.hasWon)
                            "Play again and try to solve it faster next time!"
                        else
                            "You will lose all your progress."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            sudokuHandler.generateGrid()
                        }
                    ) { Text("New Game") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Not yet") } }
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Sudoku") },
                    actions = {
                        Text(sudokuHandler.timeText)

                        var showDropDown by remember { mutableStateOf(false) }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = { showDropDown = false },
                        ) {
                            DifficultyChooser(
                                difficulty = sudokuHandler.difficulty,
                                onDifficultyChange = { sudokuHandler.difficulty = it },
                            )
                        }
                        IconButton(
                            onClick = { showDropDown = !showDropDown }
                        ) { Icon(Icons.Default.MoreVert, null) }
                    },
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
                    actions = {
                        IconToggleButton(
                            sudokuHandler.pencilIn,
                            onCheckedChange = { sudokuHandler.pencilIn = it },
                        ) {
                            Icon(
                                imageVector = if (sudokuHandler.pencilIn) Icons.Default.Edit else Icons.Default.EditOff,
                                contentDescription = null
                            )
                        }
                    },
                    floatingActionButton = {
                        NumberHighlighter(
                            chosenDigit = sudokuHandler.highlightedDigit,
                            digits = (1..sudokuHandler.dimension.uniqueDigitsCount).toList(),
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
                                digits = (1..sudokuHandler.dimension.uniqueDigitsCount).toList(),
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
                                canModify = !number.isPreset,
                                penciled = sudokuHandler.penciledInMap[SudokuCell(columnIndex, rowIndex)].orEmpty(),
                                onValueChange = {
                                    if (sudokuHandler.pencilIn) {
                                        sudokuHandler.penciledInMap[SudokuCell(columnIndex, rowIndex)]
                                            .orEmpty()
                                            .let { list ->
                                                val newList = when (it) {
                                                    0 -> emptyList()
                                                    in list -> list - it
                                                    !in list -> list + it
                                                    else -> emptyList()
                                                }

                                                sudokuHandler.penciledInMap[SudokuCell(columnIndex, rowIndex)] = newList
                                            }
                                    } else {
                                        sudokuHandler.updateGrid(it, rowIndex, columnIndex)
                                    }
                                },
                                modifier = Modifier
                                    .drawWithContent {
                                        drawContent()
                                        drawPath(
                                            drawShape(
                                                px,
                                                showBottomLine = false,
                                                showTopLine = columnIndex % sudokuHandler.dimension.boxHeight == 0 && columnIndex != 0,
                                                showLeftLine = rowIndex % sudokuHandler.dimension.boxWidth == 0 && rowIndex != 0,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Digit(
    value: Int,
    digits: List<Int>,
    onValueChange: (Int) -> Unit,
    shape: Shape,
    canModify: Boolean,
    highlight: () -> Boolean,
    penciled: List<Int>,
    modifier: Modifier = Modifier,
) {
    var showDropDown by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        if (highlight()) MaterialTheme.colorScheme.secondary
        else Color.Unspecified
    )

    OutlinedCard(
        onClick = { showDropDown = true },
        enabled = canModify,
        shape = shape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor,
            disabledContentColor = animateColorAsState(
                if (highlight()) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurface
            ).value
        ),
        border = CardDefaults.outlinedCardBorder(true),
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
                .padding(4.dp)
                .sizeIn(48.dp, 48.dp),
        ) {
            if (value == 0) {
                penciled
                    .sorted()
                    .forEach {
                        Text(
                            it.toString(),
                            fontSize = 8.sp,
                            modifier = Modifier.align(
                                when (it) {
                                    1 -> Alignment.TopStart
                                    2 -> Alignment.TopCenter
                                    3 -> Alignment.TopEnd
                                    4 -> Alignment.CenterStart
                                    5 -> Alignment.Center
                                    6 -> Alignment.CenterEnd
                                    7 -> Alignment.BottomStart
                                    8 -> Alignment.BottomCenter
                                    9 -> Alignment.BottomEnd
                                    else -> Alignment.Center
                                }
                            )
                        )
                    }
            }
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
fun DifficultyChooser(
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
) {
    var showDropDown by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = { showDropDown = false },
    ) {
        Difficulty.entries.forEach {
            DropdownMenuItem(
                text = { Text(it.name) },
                onClick = {
                    onDifficultyChange(it)
                    showDropDown = false
                },
                leadingIcon = {
                    if (difficulty == it) Icon(Icons.Default.Check, null)
                }
            )
        }
    }

    DropdownMenuItem(
        text = { Text(difficulty.name) },
        onClick = { showDropDown = true },
    )
}

@Composable
fun NumberHighlighter(
    chosenDigit: Int,
    digits: List<Int>,
    onValueChange: (Int) -> Unit,
) {
    var showDropDown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = chosenDigit,
        initialPageOffsetFraction = 0f
    ) { digits.size + 1 }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { onValueChange(it) }
    }

    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = { showDropDown = false },
    ) {
        DropdownMenuItem(
            text = { Text("Clear") },
            onClick = {
                onValueChange(0)
                scope.launch { pagerState.scrollToPage(0) }
                showDropDown = false
            },
        )
        digits.forEach { grade ->
            DropdownMenuItem(
                text = { Text(grade.toString()) },
                onClick = {
                    onValueChange(grade)
                    scope.launch { pagerState.scrollToPage(grade) }
                    showDropDown = false
                },
            )
        }
    }

    VerticalPager(
        state = pagerState
    ) {
        ExtendedFloatingActionButton(
            onClick = { showDropDown = !showDropDown },
            text = { Text(it.toString()) },
            icon = { Text("Highlight") }
        )
    }
}

@OptIn(ExperimentalSudoklifyApi::class)
class SudokuHandler : ViewModel() {
    private val architect = SudoklifyArchitect { loadPresetSchemas() }
    private var sudokuSpec: SudokuSpec = SudokuSpec {
        seed = Seed.Random()
        type = Dimension.NineByNine
    }

    var difficulty by mutableStateOf(Difficulty.EASY)
    var pencilIn by mutableStateOf(false)

    lateinit var puzzle: SudokuPuzzle

    var dimension = sudokuSpec.type
    var size = dimension.uniqueDigitsCount - 1

    var highlightedDigit by mutableIntStateOf(0)

    var hasWon by mutableStateOf(false)
    private val stopwatch = Stopwatch(tick = 1L)
    private var time by mutableLongStateOf(0)
    private val minutes by derivedStateOf { time.milliseconds.inWholeMinutes }
    private val seconds by derivedStateOf { (time - minutes * 60 * 1000).milliseconds.inWholeSeconds }
    val timeText by derivedStateOf {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    val generatedGrid: SnapshotStateList<SnapshotStateList<SudokuDigit>> = mutableStateListOf()

    val penciledInMap = mutableStateMapOf<SudokuCell, List<Int>>()

    init {
        snapshotFlow { hasWon }
            .onEach {
                if (it) stopwatch.pause()
            }
            .launchIn(viewModelScope)

        stopwatch.time
            .onEach { time++ }
            .launchIn(viewModelScope)

        generateGrid()
    }

    fun generateGrid(
        difficulty: Difficulty = this.difficulty,
    ) {
        time = 0
        hasWon = false

        penciledInMap.clear()

        sudokuSpec = SudokuSpec {
            seed = Seed.Random()
            type = Dimension.NineByNine
            this.difficulty = difficulty
        }

        dimension = sudokuSpec.type
        size = dimension.uniqueDigitsCount - 1

        puzzle = architect.constructSudoku(sudokuSpec)

        generatedGrid.clear()
        generatedGrid.addAll(
            puzzle.generateGridWithGivens()
                .mapIndexed { rowIndex, rowList ->
                    rowList.mapIndexed { colIndex, number ->
                        val solutionValue = puzzle.solution[rowIndex][colIndex]
                        SudokuDigit(
                            number = number,
                            solution = solutionValue,
                            isPreset = number != 0,
                        )
                    }.toMutableStateList()
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
        generatedGrid[column][row] = generatedGrid[column][row].copy(number = number)

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
    val solution: Int,
    val isPreset: Boolean = false,
)

data class SudokuCell(
    val row: Int,
    val col: Int,
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
