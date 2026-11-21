export type Post = {
  id: string;
  handle: string;
  url: string;
};

export type SavedPost = Post & { savedAt: number };

type ParsedShare =
  | { kind: 'post'; post: Post }
  | { kind: 'missing-handle' | 'unsupported' | 'ambiguous' };

const postUrl =
  /^https?:\/\/(?:www\.|mobile\.|m\.)?(?:x\.com|twitter\.com)\/(?:i\/web|([a-z0-9_]{1,15}))\/status\/([1-9]\d{0,19})(?:\/(?:photo|video)\/\d+)?\/?(?:[?#].*)?$/i;

export function parseShare(texts: readonly string[]): ParsedShare {
  let post: Post | undefined;
  let statusId: string | undefined;

  for (const text of texts) {
    const urls = text.match(/\bhttps?:\/\/[^\s<>"'…]+/gi) ?? [];
    for (const url of urls) {
      const match = postUrl.exec(url.replace(/[)\]}.,;:!?]+$/, ''));
      if (!match) continue;

      const [, handle, id] = match;
      if (statusId && statusId !== id) return { kind: 'ambiguous' };
      statusId = id;
      if (!handle || handle.toLowerCase() === 'i') continue;

      if (post && post.handle.toLowerCase() !== handle.toLowerCase()) {
        return { kind: 'ambiguous' };
      }
      post ??= { id, handle, url: `https://x.com/${handle}/status/${id}` };
    }
  }

  if (post) return { kind: 'post', post };
  return { kind: statusId ? 'missing-handle' : 'unsupported' };
}
