package app.xmanager.followlog

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

private const val CREATE_POSTS = """
  CREATE TABLE IF NOT EXISTS posts (
    id TEXT PRIMARY KEY NOT NULL,
    handle TEXT NOT NULL,
    url TEXT NOT NULL,
    saved_at INTEGER NOT NULL
  )
"""

private const val INSERT_POST = """
  INSERT OR IGNORE INTO posts (id, handle, url, saved_at) VALUES (?, ?, ?, ?)
"""

private const val LIST_POSTS = """
  SELECT id, handle, url, saved_at FROM posts ORDER BY saved_at DESC, rowid DESC
"""

internal object FollowLogStore {
  val listeners = CopyOnWriteArraySet<() -> Unit>()
  private var connection: SQLiteDatabase? = null

  @Synchronized
  private fun database(context: Context): SQLiteDatabase {
    connection?.let { return it }

    // This is the database path that expo-sqlite used for the first APK.
    val directory = File(context.filesDir, "SQLite")
    check(directory.isDirectory || directory.mkdirs())
    val opened = SQLiteDatabase.openDatabase(
      File(directory, "follow-reasons.db").path,
      null,
      SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
      DatabaseErrorHandler { /* Keep the user's database if opening it fails. */ },
    )
    try {
      opened.execSQL(CREATE_POSTS)
    } catch (error: Exception) {
      opened.close()
      throw error
    }
    connection = opened
    return opened
  }

  fun save(context: Context, post: ShareResult.Post, savedAt: Long): Boolean =
    database(context).compileStatement(INSERT_POST).use { statement ->
      statement.bindString(1, post.id)
      statement.bindString(2, post.handle)
      statement.bindString(3, post.url)
      statement.bindLong(4, savedAt)
      statement.executeInsert() != -1L
    }

  fun list(context: Context): List<Map<String, Any>> = database(context).rawQuery(LIST_POSTS, null).use { rows ->
    buildList {
      while (rows.moveToNext()) {
        add(mapOf(
          "id" to rows.getString(0),
          "handle" to rows.getString(1),
          "url" to rows.getString(2),
          "savedAt" to rows.getLong(3).toDouble(),
        ))
      }
    }
  }
}
