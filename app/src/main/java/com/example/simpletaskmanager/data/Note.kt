package com.example.simpletaskmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single task / note.
 *
 * SECURE CODING NOTE 2 – Input storage:
 *   All text fields (title, description) are stored as plain Strings inside
 *   Room's SQLite database.  The database file itself is placed in the app's
 *   private internal storage directory which is sandbox-protected by Android's
 *   permission model (no other app can read it without root access).
 *   We intentionally do NOT store any personally-identifiable information or
 *   credentials inside this entity – tasks are purely user-generated content.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    /** Short summary of the task – must not be blank (validated in UI layer). */
    val title: String,

    /** Optional longer description of the task. */
    val description: String,

    /** Whether the user has marked this task as done. */
    val isCompleted: Boolean = false,

    /**
     * Priority level encoded as an integer for compact storage.
     * 0 = Low  |  1 = Medium  |  2 = High
     */
    val priority: Int = 1,

    /** Creation timestamp (epoch milliseconds) – used for default sort order. */
    val dateCreated: Long = System.currentTimeMillis()
)
