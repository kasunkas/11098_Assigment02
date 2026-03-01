package com.example.simpletaskmanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpletaskmanager.data.Note
import com.example.simpletaskmanager.ui.theme.PriorityHigh
import com.example.simpletaskmanager.ui.theme.PriorityLow
import com.example.simpletaskmanager.ui.theme.PriorityMedium
import com.example.simpletaskmanager.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── Priority helpers ────────────────────────────────────────────────────────

/** Maps an integer priority code to a display label. */
fun priorityLabel(priority: Int) = when (priority) {
    2    -> "High"
    1    -> "Medium"
    else -> "Low"
}

/** Maps an integer priority code to a colour. */
fun priorityColor(priority: Int) = when (priority) {
    2    -> PriorityHigh
    1    -> PriorityMedium
    else -> PriorityLow
}

// ─── Main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    viewModel:    TaskViewModel,
    isSystemDark: Boolean
) {
    val tasks          by viewModel.tasks.collectAsState()
    val isSortAscending by viewModel.sortAscending.collectAsState()
    val isDarkTheme    by viewModel.isDarkTheme.collectAsState()

    // Dialog state
    var showDialog  by remember { mutableStateOf(false) }
    var noteToEdit  by remember { mutableStateOf<Note?>(null) }

    // Computed stats
    val total     = tasks.size
    val completed = tasks.count { it.isCompleted }

    // Effective dark flag for toggling
    val effectiveDark = isDarkTheme ?: isSystemDark

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "My Tasks",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // ── Dark / Light mode toggle ──────────────────────────
                    IconButton(onClick = { viewModel.toggleTheme(isSystemDark) }) {
                        Text(
                            text     = if (effectiveDark) "☀" else "🌙",
                            fontSize = 20.sp
                        )
                    }
                    // ── Sort order toggle ─────────────────────────────────
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            imageVector        = if (isSortAscending)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isSortAscending) "Sort newest first" else "Sort oldest first"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick            = { noteToEdit = null; showDialog = true },
                containerColor     = MaterialTheme.colorScheme.primary,
                contentColor       = MaterialTheme.colorScheme.onPrimary,
                shape              = CircleShape,
                modifier           = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Stats banner ──────────────────────────────────────────────
            if (total > 0) {
                StatsBanner(total = total, completed = completed)
            }

            // ── Task list or empty state ──────────────────────────────────
            if (tasks.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = tasks,
                        key   = { it.id }
                    ) { note ->
                        AnimatedVisibility(
                            visible  = true,
                            enter    = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f),
                            modifier = Modifier.animateItemPlacement()
                        ) {
                            TaskCard(
                                note              = note,
                                onToggleComplete  = { viewModel.toggleComplete(note) },
                                onDelete          = { viewModel.deleteNote(note) },
                                onClick           = { noteToEdit = note; showDialog = true }
                            )
                        }
                    }

                    // Bottom padding so FAB doesn't overlap last card
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // ── Add / Edit dialog ─────────────────────────────────────────────
        if (showDialog) {
            AddEditNoteDialog(
                note      = noteToEdit,
                onDismiss = { showDialog = false },
                onSave    = { title, desc, priority ->
                    if (noteToEdit == null) {
                        viewModel.addNote(title, desc, priority)
                    } else {
                        viewModel.updateNote(
                            noteToEdit!!.copy(
                                title       = title,
                                description = desc,
                                priority    = priority
                            )
                        )
                    }
                    showDialog = false
                }
            )
        }
    }
}

// ─── Stats banner ─────────────────────────────────────────────────────────────

@Composable
private fun StatsBanner(total: Int, completed: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Surface(
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape         = RoundedCornerShape(16.dp),
        color         = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text  = "$completed / $total tasks done",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text  = if (completed == total && total > 0) "All done! 🎉"
                                else "${total - completed} remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                // Circular progress indicator
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress         = { progress },
                        modifier         = Modifier.size(52.dp),
                        color            = MaterialTheme.colorScheme.primary,
                        trackColor       = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        strokeWidth      = 5.dp
                    )
                    Text(
                        text  = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress     = { progress },
                modifier     = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color        = MaterialTheme.colorScheme.primary,
                trackColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(
        modifier          = Modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "📋", fontSize = 64.sp)
            Text(
                text  = "No tasks yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = "Tap the + button to add your first task",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Task card ────────────────────────────────────────────────────────────────

@Composable
fun TaskCard(
    note:             Note,
    onToggleComplete: () -> Unit,
    onDelete:         () -> Unit,
    onClick:          () -> Unit,
    modifier:         Modifier = Modifier
) {
    // Animate card background when completed
    val cardBg by animateColorAsState(
        targetValue = if (note.isCompleted)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "cardBg"
    )

    val pColor = priorityColor(note.priority)

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Ensure children can fill height
        ) {

            // ── Priority accent bar on the left ───────────────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight() // Match the tallest sibling
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(pColor, pColor.copy(alpha = 0.5f))
                        )
                    )
                    .defaultMinSize(minHeight = 80.dp)
            )

            // ── Card content ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
            ) {
                // Title row
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.SpaceBetween,
                    modifier               = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text      = note.title,
                        style     = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier.weight(1f),
                        // Strike-through when completed
                        textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null,
                        color = if (note.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Priority chip
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text     = priorityLabel(note.priority),
                            color    = pColor,
                            style    = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Description
                if (note.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text      = note.description,
                        style     = MaterialTheme.typography.bodyMedium,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis,
                        color     = if (note.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date + action row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = SimpleDateFormat(
                            "MMM dd, yyyy", Locale.getDefault()
                        ).format(Date(note.dateCreated)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Complete toggle
                        IconButton(
                            onClick  = onToggleComplete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector        = if (note.isCompleted)
                                    Icons.Filled.CheckCircle
                                else
                                    Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (note.isCompleted)
                                    "Mark incomplete" else "Mark complete",
                                tint               = if (note.isCompleted)
                                    PriorityLow
                                else
                                    MaterialTheme.colorScheme.outline,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                        // Delete
                        IconButton(
                            onClick  = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Delete,
                                contentDescription = "Delete task",
                                tint               = MaterialTheme.colorScheme.error,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Add / Edit dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteDialog(
    note:      Note?,
    onDismiss: () -> Unit,
    onSave:    (title: String, description: String, priority: Int) -> Unit
) {
    /*
     * SECURE CODING NOTE – Input validation:
     *   We validate on the client side before allowing [onSave] to execute.
     *   Title must be non-blank and no longer than 200 characters.
     *   Description is capped at 2000 characters in the TextField itself.
     *   This prevents oversized strings from reaching Room and protects
     *   against accidental UI overflow or UI-thread slowdowns.
     */
    var title       by remember { mutableStateOf(note?.title       ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    var priority    by remember { mutableIntStateOf(note?.priority  ?: 1) }
    var titleError  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest  = onDismiss,
        shape             = RoundedCornerShape(20.dp),
        containerColor    = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text       = if (note == null) "New Task" else "Edit Task",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Title field ───────────────────────────────────────────
                OutlinedTextField(
                    value       = title,
                    onValueChange = {
                        // Limit title to 200 chars (input validation)
                        if (it.length <= 200) { title = it; titleError = false }
                    },
                    label       = { Text("Title *") },
                    isError     = titleError,
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    shape       = RoundedCornerShape(12.dp),
                    supportingText = if (titleError) {
                        { Text("Title cannot be empty", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // ── Description field ─────────────────────────────────────
                OutlinedTextField(
                    value         = description,
                    onValueChange = {
                        // Limit description to 2000 chars (input validation)
                        if (it.length <= 2000) description = it
                    },
                    label     = { Text("Description (optional)") },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines  = 5,
                    shape     = RoundedCornerShape(12.dp)
                )

                // ── Priority selector ─────────────────────────────────────
                Text(
                    text  = "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Low", 1 to "Medium", 2 to "High").forEach { (value, label) ->
                        val selected = priority == value
                        val pColor   = priorityColor(value)
                        FilterChip(
                            selected = selected,
                            onClick  = { priority = value },
                            label    = {
                                Text(
                                    text  = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor    = pColor.copy(alpha = 0.2f),
                                selectedLabelColor        = pColor,
                                selectedLeadingIconColor  = pColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled          = true,
                                selected         = selected,
                                selectedBorderColor = pColor,
                                borderColor      = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(title.trim(), description.trim(), priority)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape   = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
