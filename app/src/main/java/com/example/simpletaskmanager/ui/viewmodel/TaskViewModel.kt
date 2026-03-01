package com.example.simpletaskmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simpletaskmanager.data.Note
import com.example.simpletaskmanager.data.NoteDao
import com.example.simpletaskmanager.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [TaskViewModel] is the single source of truth for UI state.
 *
 * It bridges the data layer ([NoteDao], [UserPreferencesRepository]) with the
 * Compose UI.  Using a ViewModel guarantees that all state survives screen
 * rotation and configuration changes – satisfying the assignment's
 * "Handle screen rotation properly" requirement.
 */
class TaskViewModel(
    private val noteDao: NoteDao,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    // ─── Sort order ──────────────────────────────────────────────────────────

    /** True = oldest first; False = newest first (default). */
    private val _sortAscending = MutableStateFlow(prefs.isSortAscending)
    val sortAscending: StateFlow<Boolean> = _sortAscending

    // ─── Dark / Light mode ───────────────────────────────────────────────────

    /**
     * Nullable dark-mode flag.
     *  null  → follow system
     *  true  → force dark
     *  false → force light
     *
     * Read once from encrypted prefs at startup; then kept in memory so Compose
     * can react to it instantly without disk I/O on every recomposition.
     */
    private val _isDarkTheme = MutableStateFlow<Boolean?>(prefs.getThemeOverride())
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme

    fun toggleTheme(isSystemDark: Boolean) {
        // If there's no override yet, treat current effective theme as the starting point
        val current = _isDarkTheme.value ?: isSystemDark
        val next = !current
        _isDarkTheme.value = next
        viewModelScope.launch { prefs.saveThemeOverride(next) }
    }

    // ─── Task list ───────────────────────────────────────────────────────────

    /**
     * Live-sorted list of all tasks, automatically updated whenever the
     * database or sort preference changes.
     */
    val tasks: StateFlow<List<Note>> = combine(
        noteDao.getAllNotes(),
        _sortAscending
    ) { notes, isAsc ->
        if (isAsc) notes.sortedBy { it.dateCreated }
        else        notes.sortedByDescending { it.dateCreated }
    }.stateIn(
        scope          = viewModelScope,
        started        = SharingStarted.WhileSubscribed(5_000),
        initialValue   = emptyList()
    )

    // ─── Actions ─────────────────────────────────────────────────────────────

    fun toggleSortOrder() {
        val new = !_sortAscending.value
        _sortAscending.value = new
        viewModelScope.launch { prefs.saveSortOrder(new) }
    }

    /**
     * Adds a new task.
     *
     * SECURE CODING NOTE – Input validation:
     *   [title] is expected to be non-blank; this is enforced in the UI layer
     *   (AddEditNoteDialog) before this function is ever called.  Keeping
     *   validation close to the input point prevents invalid data from reaching
     *   the database, which is a defence-in-depth approach.
     */
    fun addNote(title: String, description: String, priority: Int) {
        viewModelScope.launch {
            noteDao.insert(
                Note(
                    title       = title.trim(),
                    description = description.trim(),
                    priority    = priority
                )
            )
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch { noteDao.update(note) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteDao.delete(note) }
    }

    /** Flips the completed flag without opening the edit dialog. */
    fun toggleComplete(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.copy(isCompleted = !note.isCompleted))
        }
    }
}

// ─── Factory ─────────────────────────────────────────────────────────────────

class TaskViewModelFactory(
    private val noteDao: NoteDao,
    private val prefs:   UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(noteDao, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
