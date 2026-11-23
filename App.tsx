import { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Linking,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { color } from './src/colors';
import type { SavedPost } from './src/posts-db';
import { ScrollTimerScreen } from './src/ScrollTimerScreen';
import { useFollowLog } from './src/use-follow-log';

const savedDate = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function PostRow({ post, onOpen, onRemove }: {
  post: SavedPost;
  onOpen: () => void;
  onRemove: () => void;
}) {
  return (
    <View style={styles.row}>
      <Pressable
        accessibilityRole="link"
        accessibilityHint="Opens the original post"
        android_ripple={{ color: color.greenWash }}
        onPress={onOpen}
        style={styles.postLink}
      >
        <View style={styles.rowText}>
          <Text style={styles.handle}>@{post.handle}</Text>
          <Text style={styles.metadata}>Saved {savedDate.format(post.savedAt)}</Text>
        </View>
        <Text accessible={false} importantForAccessibility="no" style={styles.arrow}>↗</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Remove saved post by @${post.handle}`}
        android_ripple={{ color: color.rustWash }}
        onPress={onRemove}
        style={styles.removeButton}
      >
        <Text style={styles.removeLabel}>Remove</Text>
      </Pressable>
    </View>
  );
}

function FollowLog() {
  const [showTimer, setShowTimer] = useState(false);
  const [query, setQuery] = useState('');
  const [openError, setOpenError] = useState(false);
  const closeTimer = useCallback(() => setShowTimer(false), []);
  const { posts, loading, loadError, available, retryLoad, removePost } = useFollowLog();
  const accountFilter = query.trim().replace(/^@/, '').toLowerCase();
  const visiblePosts = useMemo(
    () => posts.filter(({ handle }) => handle.toLowerCase().includes(accountFilter)),
    [posts, accountFilter],
  );

  async function openPost(url: string) {
    setOpenError(false);
    try {
      await Linking.openURL(url);
    } catch {
      setOpenError(true);
    }
  }

  function confirmRemove(post: SavedPost) {
    Alert.alert(
      'Remove saved post?',
      `Remove the post by @${post.handle} saved on ${savedDate.format(post.savedAt)} from X Manager?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Remove',
          onPress: () => {
            void removePost(post.id).catch(() => {
              Alert.alert('Couldn’t remove saved post', 'Try again.');
            });
          },
        },
      ],
    );
  }

  if (showTimer) {
    return (
      <SafeAreaView style={styles.screen}>
        <StatusBar barStyle="dark-content" />
        <ScrollTimerScreen onBack={closeTimer} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.header}>
        <View style={styles.headerBar}>
          <Text style={styles.appName}>X MANAGER</Text>
          <Pressable accessibilityRole="button" onPress={() => setShowTimer(true)} style={styles.timerButton}>
            <Text style={styles.timerLabel}>Scroll timer</Text>
          </Pressable>
        </View>
        <Text accessibilityRole="header" style={styles.title}>Follow reasons</Text>
        <View style={styles.search}>
          <View accessible={false} style={styles.searchIcon}>
            <View style={styles.searchCircle} />
            <View style={styles.searchStem} />
          </View>
          <TextInput
            accessibilityLabel="Search by account handle"
            autoCapitalize="none"
            autoCorrect={false}
            placeholder="Search by account"
            placeholderTextColor={color.muted}
            returnKeyType="search"
            onChangeText={setQuery}
            value={query}
            style={styles.searchInput}
          />
          {query !== '' && (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Clear search"
              onPress={() => setQuery('')}
              style={styles.clearButton}
            >
              <Text style={styles.dismiss}>×</Text>
            </Pressable>
          )}
        </View>
        {openError && (
          <View style={styles.notice}>
            <Text accessibilityLiveRegion="polite" style={styles.noticeText}>
              Could not open this post. Try again.
            </Text>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Dismiss message"
              onPress={() => setOpenError(false)}
              style={styles.clearButton}
            >
              <Text style={styles.dismiss}>×</Text>
            </Pressable>
          </View>
        )}
      </View>
      <FlatList
        data={visiblePosts}
        keyExtractor={({ id }) => id}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <View style={styles.listHeader}>
            <Text accessibilityRole="header" style={styles.sectionName}>
              {accountFilter ? 'Search results' : 'Saved posts'}
            </Text>
            {!loading && !loadError && (
              <Text style={styles.count}>{visiblePosts.length}</Text>
            )}
          </View>
        }
        ListEmptyComponent={
          loading ? (
            <ActivityIndicator accessibilityLabel="Loading saved posts" color={color.green} style={styles.loading} />
          ) : loadError ? (
            <View style={styles.empty}>
              <Text style={styles.emptyTitle}>Couldn’t load saved posts</Text>
              <Pressable accessibilityRole="button" onPress={retryLoad} style={styles.retry}>
                <Text style={styles.retryLabel}>Try again</Text>
              </Pressable>
            </View>
          ) : (
            <View style={styles.empty}>
              <Text style={styles.emptyTitle}>{accountFilter ? 'No matching accounts' : 'No follow reasons yet'}</Text>
              <Text style={styles.emptyBody}>
                {!available
                  ? 'Saved posts are available in the X Manager Android APK.'
                  : accountFilter
                    ? 'Try a different account handle.'
                    : 'Open a post in X, tap Share, and choose X Manager from the Android share sheet.'}
              </Text>
            </View>
          )
        }
        renderItem={({ item }) => (
          <PostRow
            post={item}
            onOpen={() => void openPost(item.url)}
            onRemove={() => confirmRemove(item)}
          />
        )}
      />
    </SafeAreaView>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <FollowLog />
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.paper },
  header: { paddingHorizontal: 24, paddingTop: 10, paddingBottom: 20 },
  headerBar: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' },
  appName: { color: color.green, fontSize: 12, fontWeight: '700', letterSpacing: 2 },
  timerButton: { justifyContent: 'center', minHeight: 48, paddingLeft: 12 },
  timerLabel: { color: color.green, fontSize: 14, fontWeight: '700' },
  title: {
    color: color.ink,
    fontFamily: 'serif',
    fontSize: 39,
    fontWeight: '600',
    marginTop: 6,
    marginBottom: 22,
  },
  search: {
    minHeight: 52,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: color.card,
    borderColor: color.line,
    borderWidth: 1,
    borderRadius: 10,
  },
  searchIcon: { width: 20, height: 20, marginLeft: 15, marginRight: 9 },
  searchCircle: { width: 13, height: 13, top: 2, borderWidth: 1.5, borderRadius: 7, borderColor: color.green },
  searchStem: {
    position: 'absolute',
    width: 7,
    height: 1.5,
    top: 14,
    left: 10,
    backgroundColor: color.green,
    transform: [{ rotate: '45deg' }],
  },
  searchInput: { flex: 1, paddingVertical: 10, paddingRight: 12, color: color.ink, fontSize: 16 },
  clearButton: { minWidth: 48, minHeight: 48, alignItems: 'center', justifyContent: 'center' },
  dismiss: { color: color.ink, fontSize: 24 },
  notice: {
    marginTop: 14,
    borderRadius: 8,
    backgroundColor: color.rustWash,
    alignItems: 'center',
    flexDirection: 'row',
  },
  noticeText: { flex: 1, color: color.rust, fontSize: 14, paddingVertical: 12, paddingLeft: 12 },
  list: { paddingHorizontal: 24, paddingBottom: 24 },
  listHeader: {
    minHeight: 40,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: color.muted,
  },
  sectionName: { color: color.ink, fontSize: 15, fontWeight: '700' },
  count: { color: color.muted, fontSize: 13, fontVariant: ['tabular-nums'] },
  row: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: color.line,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  postLink: {
    flex: 1,
    minHeight: 76,
    paddingVertical: 13,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  rowText: { flex: 1, gap: 4 },
  handle: { color: color.ink, fontSize: 18, fontWeight: '700' },
  metadata: { color: color.muted, fontSize: 14 },
  arrow: { color: color.green, fontSize: 22, paddingRight: 4 },
  removeButton: { minHeight: 48, paddingHorizontal: 8, justifyContent: 'center' },
  removeLabel: { color: color.rust, fontSize: 14, fontWeight: '700' },
  loading: { paddingVertical: 32 },
  empty: { paddingTop: 32, paddingBottom: 12, gap: 10 },
  emptyTitle: { color: color.ink, fontFamily: 'serif', fontSize: 24 },
  emptyBody: { color: color.muted, fontSize: 16, lineHeight: 24, maxWidth: 320 },
  retry: { alignSelf: 'flex-start', minHeight: 48, justifyContent: 'center' },
  retryLabel: { color: color.green, fontSize: 16, fontWeight: '700' },
});
