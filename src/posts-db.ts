import { requireOptionalNativeModule, type EventSubscription } from 'expo';

export type SavedPost = {
  id: string;
  handle: string;
  url: string;
  savedAt: number;
};

export const followLog = requireOptionalNativeModule<{
  listPosts(): Promise<SavedPost[]>;
  addListener(event: 'onPostsChanged', listener: () => void): EventSubscription;
}>('XManagerFollowLog');
