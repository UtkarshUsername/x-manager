import { useCallback, useEffect, useRef, useState } from 'react';
import { AppState } from 'react-native';
import { followLog, type SavedPost } from './posts-db';

export function useFollowLog() {
  const [posts, setPosts] = useState<SavedPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const latest = useRef(0);

  const refresh = useCallback(async () => {
    const request = ++latest.current;
    try {
      const saved = (await followLog?.listPosts()) ?? [];
      if (request !== latest.current) return;
      setPosts(saved);
      setLoadError(false);
    } catch {
      if (request === latest.current) setLoadError(true);
    } finally {
      if (request === latest.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const changed = followLog?.addListener('onPostsChanged', () => void refresh());
    const appState = AppState.addEventListener('change', (next) => {
      if (next === 'active') void refresh();
    });
    void refresh();
    return () => {
      latest.current++;
      changed?.remove();
      appState.remove();
    };
  }, [refresh]);

  return {
    posts,
    loading,
    loadError,
    available: followLog !== null,
    removePost: async (id: string) => {
      if (!followLog) throw new Error('Saved posts are unavailable');
      await followLog.removePost(id);
      setPosts((current) => current.filter((post) => post.id !== id));
      void refresh();
    },
    retryLoad: () => {
      setLoading(true);
      void refresh();
    },
  };
}
