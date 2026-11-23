import { requireOptionalNativeModule, type EventSubscription } from 'expo';

export type SavedPost = {
  id: string;
  handle: string;
  url: string;
  savedAt: number;
};

export const followLog = requireOptionalNativeModule<{
  listPosts(): Promise<SavedPost[]>;
  removePost(id: string): Promise<void>;
  addListener(event: 'onPostsChanged', listener: () => void): EventSubscription;
}>('XManagerFollowLog');
