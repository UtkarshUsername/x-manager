import { openDatabaseAsync, type SQLiteDatabase } from 'expo-sqlite';
import type { SavedPost } from './post';

let database: Promise<SQLiteDatabase> | undefined;

function getDatabase() {
  database ??= initialize().catch((error) => {
    database = undefined;
    throw error;
  });
  return database;
}

async function initialize() {
  const db = await openDatabaseAsync('follow-reasons.db');
  try {
    await db.execAsync(`
      CREATE TABLE IF NOT EXISTS posts (
        id TEXT PRIMARY KEY NOT NULL,
        handle TEXT NOT NULL,
        url TEXT NOT NULL,
        saved_at INTEGER NOT NULL
      );
    `);
    return db;
  } catch (error) {
    await db.closeAsync();
    throw error;
  }
}

export async function listPosts() {
  const db = await getDatabase();
  return db.getAllAsync<SavedPost>(`
    SELECT id, handle, url, saved_at AS savedAt
    FROM posts
    ORDER BY saved_at DESC, rowid DESC
  `);
}

export async function savePost({ id, handle, url, savedAt }: SavedPost) {
  const db = await getDatabase();
  const { changes } = await db.runAsync(
    `INSERT INTO posts (id, handle, url, saved_at)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(id) DO NOTHING`,
    id,
    handle,
    url,
    savedAt,
  );
  return changes === 1;
}
