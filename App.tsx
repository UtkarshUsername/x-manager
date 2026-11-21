import { useMemo } from 'react';
import {
  ActivityIndicator,
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
import type { SavedPost } from './src/post';
import { useFollowLog } from './src/use-follow-log';

const color = {
  paper: '#F5F3EC',
  card: '#FFFDF7',
  ink: '#14251E',
  muted: '#526359',
  line: '#D7DCD2',
  green: '#1C5A3F',
  greenWash: '#E0EEE0',
  rust: '#842F21',
  rustWash: '#F6E5DC',
};

const savedDate = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function PostRow({ post, onOpen }: { post: SavedPost; onOpen: () => void }) {
  return (
    <Pressable
      accessibilityRole="link"
      accessibilityHint="Opens the original post"
      android_ripple={{ color: color.greenWash }}
      onPress={onOpen}
      style={styles.row}
    >
      <View style={styles.rowText}>
        <Text style={styles.handle}>@{post.handle}</Text>
        <Text style={styles.metadata}>Saved {savedDate.format(post.savedAt)}</Text>
      </View>
      <Text accessible={false} importantForAccessibility="no" style={styles.arrow}>↗</Text>
    </Pressable>
  );
}

function FollowLog() {
  const { posts, loading, loadError, notice, setNotice, query, setQuery, retryLoad } = useFollowLog();
  const accountFilter = query.trim().replace(/^@/, '').toLowerCase();
  const visiblePosts = useMemo(
    () => posts.filter(({ handle }) => handle.toLowerCase().includes(accountFilter)),
    [posts, accountFilter],
  );

  async function openPost(url: string) {
    try {
      await Linking.openURL(url);
    } catch {
      setNotice({ message: 'Could not open this post. Try again.', tone: 'error' });
    }
  }

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.header}>
        <Text style={styles.appName}>X MANAGER</Text>
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
        {notice && (
          <View style={[styles.notice, notice.tone === 'error' && styles.errorNotice]}>
            <Text
              accessibilityLiveRegion="polite"
              style={[styles.noticeText, notice.tone === 'error' && styles.errorNoticeText]}
            >
              {notice.message}
            </Text>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Dismiss message"
              onPress={() => setNotice(null)}
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
                {accountFilter
                  ? 'Try a different account handle.'
                  : 'Open a post in X, tap Share, and choose X Manager from the Android share sheet.'}
              </Text>
            </View>
          )
        }
        renderItem={({ item }) => <PostRow post={item} onOpen={() => void openPost(item.url)} />}
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
  header: { paddingHorizontal: 24, paddingTop: 24, paddingBottom: 20 },
  appName: { color: color.green, fontSize: 12, fontWeight: '700', letterSpacing: 2 },
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
    backgroundColor: color.greenWash,
    alignItems: 'center',
    flexDirection: 'row',
  },
  errorNotice: { backgroundColor: color.rustWash },
  noticeText: { flex: 1, color: color.green, fontSize: 14, paddingVertical: 12, paddingLeft: 12 },
  errorNoticeText: { color: color.rust },
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
    minHeight: 76,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: color.line,
    paddingVertical: 13,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  rowText: { flex: 1, gap: 4 },
  handle: { color: color.ink, fontSize: 18, fontWeight: '700' },
  metadata: { color: color.muted, fontSize: 14 },
  arrow: { color: color.green, fontSize: 22, paddingRight: 4 },
  loading: { paddingVertical: 32 },
  empty: { paddingTop: 32, paddingBottom: 12, gap: 10 },
  emptyTitle: { color: color.ink, fontFamily: 'serif', fontSize: 24 },
  emptyBody: { color: color.muted, fontSize: 16, lineHeight: 24, maxWidth: 320 },
  retry: { alignSelf: 'flex-start', minHeight: 48, justifyContent: 'center' },
  retryLabel: { color: color.green, fontSize: 16, fontWeight: '700' },
});
