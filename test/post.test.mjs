import assert from 'node:assert/strict';
import test from 'node:test';
import { parseShare } from '../src/post.ts';

const id = '9007199254740993';
const post = {
  kind: 'post',
  post: { id, handle: 'Some_Account', url: `https://x.com/Some_Account/status/${id}` },
};

test('finds the post in text and removes tracking parameters', () => {
  assert.deepEqual(
    parseShare([`Interesting post (https://x.com/Some_Account/status/${id}?s=20&t=token).`]),
    post,
  );
});

test('gives legacy Twitter media links the same ID and canonical URL as X links', () => {
  assert.deepEqual(
    parseShare([`https://mobile.twitter.com/Some_Account/status/${id}/photo/1?ref_src=share`]),
    post,
  );
});

test('accepts repeated copies of the same post across payloads', () => {
  assert.deepEqual(
    parseShare([post.post.url, `https://twitter.com/some_account/status/${id}`]),
    post,
  );
});

test('requires a handle in a post URL and ignores mentions in the share text', () => {
  for (const path of ['i/status', 'i/web/status']) {
    assert.deepEqual(
      parseShare([`@Some_Account shared this: https://x.com/${path}/${id}`]),
      { kind: 'missing-handle' },
    );
  }
});

test('rejects conflicting post IDs and conflicting handles', () => {
  for (const url of ['https://x.com/i/status/42', `https://x.com/Different/status/${id}`]) {
    assert.deepEqual(parseShare([post.post.url, url]), { kind: 'ambiguous' });
  }
});

test('rejects non-post URLs, unsupported hosts, and shares without links', () => {
  for (const text of [
    'https://x.com/Some_Account',
    'https://x.com/Some_Account/status/abc',
    `https://x.com.example.org/Some_Account/status/${id}`,
    `https://example.org/?url=${post.post.url}`,
    'https://t.co/shortlink',
    'Plain text with no link',
  ]) {
    assert.deepEqual(parseShare([text]), { kind: 'unsupported' });
  }
  assert.deepEqual(parseShare([]), { kind: 'unsupported' });
});
