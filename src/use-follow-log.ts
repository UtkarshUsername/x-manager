import { clearSharedPayloads, getSharedPayloads } from 'expo-sharing';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Linking } from 'react-native';
import { parseShare, type SavedPost } from './post';
import { listPosts, savePost } from './posts-db';

type Notice = { message: string; tone: 'success' | 'info' | 'error' };

const shareErrors = {
  'missing-handle': 'This post link has no account handle. Share a link that includes the handle.',
  unsupported: 'Share an X or Twitter post link to save it here.',
  ambiguous: 'This share has conflicting post links. Share one post link at a time.',
};

function isShareUrl(url: string | null | undefined) {
  return url?.endsWith('://expo-sharing') === true;
}

export function useFollowLog() {
  const [posts, setPosts] = useState<SavedPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [query, setQuery] = useState('');
  const queue = useRef(Promise.resolve());

  const refresh = useCallback(async () => {
    try {
      setPosts(await listPosts());
      setLoadError(false);
    } catch {
      setLoadError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    let receivedWarmShare = false;
    queue.current = queue.current.then(refresh);

    function receiveShare(expected: boolean) {
      if (!active) return;
      const savedAt = Date.now();
      let result: ReturnType<typeof parseShare>;
      try {
        const payloads = getSharedPayloads();
        if (payloads.length === 0 && !expected) return;
        // Expo has one pending intent. Copy its payload before any asynchronous work.
        clearSharedPayloads();
        result = parseShare(
          payloads
            .filter(({ shareType }) => shareType === 'text' || shareType === 'url')
            .map(({ value }) => value),
        );
      } catch {
        setNotice({ message: 'Could not read this share. Try sharing the post again.', tone: 'error' });
        return;
      }

      queue.current = queue.current.then(async () => {
        if (result.kind !== 'post') {
          setNotice({ message: shareErrors[result.kind], tone: 'error' });
          return;
        }

        const { post } = result;
        try {
          const inserted = await savePost({ ...post, savedAt });
          if (!active) return;
          setNotice({
            message: inserted ? `Saved @${post.handle}’s post.` : 'This post is already saved.',
            tone: inserted ? 'success' : 'info',
          });
          if (inserted) setQuery('');
          await refresh();
        } catch {
          if (active) {
            setNotice({ message: 'Could not save this post. Try sharing it again.', tone: 'error' });
          }
        }
      });
    }

    const subscription = Linking.addEventListener('url', ({ url }) => {
      if (!isShareUrl(url)) return;
      receivedWarmShare = true;
      receiveShare(true);
    });

    // Cold launches expose the share URL and the same pending payload as warm launches.
    void Linking.getInitialURL().then(
      (url) => {
        if (!receivedWarmShare) receiveShare(isShareUrl(url));
      },
      () => {
        if (!receivedWarmShare) receiveShare(false);
      },
    );

    return () => {
      active = false;
      subscription.remove();
    };
  }, [refresh]);

  return {
    posts,
    loading,
    loadError,
    notice,
    setNotice,
    query,
    setQuery,
    retryLoad: () => {
      setLoading(true);
      queue.current = queue.current.then(refresh);
    },
  };
}
