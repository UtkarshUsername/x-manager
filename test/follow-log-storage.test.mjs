import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { DatabaseSync } from 'node:sqlite';
import test from 'node:test';

const store = readFileSync(new URL(
  '../modules/follow-log/android/src/main/java/app/xmanager/followlog/FollowLogStore.kt',
  import.meta.url,
), 'utf8');

function statement(name) {
  const match = store.match(new RegExp(`const val ${name} = """([\\s\\S]*?)"""`));
  assert.ok(match, `Missing ${name} statement`);
  return match[1];
}

test('reads the original database and preserves the first saved timestamp', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(`
      CREATE TABLE posts (
        id TEXT PRIMARY KEY NOT NULL,
        handle TEXT NOT NULL,
        url TEXT NOT NULL,
        saved_at INTEGER NOT NULL
      );
      INSERT INTO posts VALUES ('9007199254740993', 'Example', 'https://x.com/Example/status/9007199254740993', 1000);
    `);

    db.exec(statement('CREATE_POSTS'));
    const insert = db.prepare(statement('INSERT_POST'));
    const list = db.prepare(statement('LIST_POSTS'));
    assert.equal(insert.run('9007199254740993', 'Different', 'https://x.com/Different/status/9007199254740993', 2000).changes, 0);
    assert.equal(insert.run('9007199254740994', 'Second', 'https://x.com/Second/status/9007199254740994', 3000).changes, 1);
    assert.deepEqual(list.all().map(({ id, handle, saved_at }) => [id, handle, saved_at]), [
      ['9007199254740994', 'Second', 3000],
      ['9007199254740993', 'Example', 1000],
    ]);
  } finally {
    db.close();
  }
});

test('removes only the chosen post and lets it be saved again', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(statement('CREATE_POSTS'));
    const insert = db.prepare(statement('INSERT_POST'));
    const remove = db.prepare(statement('DELETE_POST'));
    const list = db.prepare(statement('LIST_POSTS'));
    const first = '9007199254740992';
    const second = '9007199254740993';
    insert.run(first, 'Example', `https://x.com/Example/status/${first}`, 1000);
    insert.run(second, 'Example', `https://x.com/Example/status/${second}`, 2000);

    assert.equal(remove.run(first).changes, 1);
    assert.equal(remove.run(first).changes, 0);
    assert.deepEqual(list.all().map(({ id }) => id), [second]);

    assert.equal(insert.run(first, 'Example', `https://x.com/Example/status/${first}`, 3000).changes, 1);
    assert.deepEqual(list.all().map(({ id, saved_at }) => [id, saved_at]), [
      [first, 3000],
      [second, 2000],
    ]);
  } finally {
    db.close();
  }
});
