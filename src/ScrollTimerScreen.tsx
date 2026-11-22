import { requireOptionalNativeModule } from 'expo';
import { useEffect, useState } from 'react';
import {
  AppState,
  BackHandler,
  Linking,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { color } from './colors';

type TimerState = {
  enabled: boolean;
  granted: boolean;
  running: boolean;
  locked: boolean;
  foregroundMode: boolean;
  totalSeconds: number;
  sawScrollEvents: boolean;
  overlayFailed: boolean;
};

const timer = requireOptionalNativeModule<{
  getState(): Promise<TimerState>;
  setEnabled(enabled: boolean): Promise<TimerState>;
  setForegroundMode(foreground: boolean): Promise<TimerState>;
  resetTime(): Promise<TimerState>;
}>('XManagerScrollTimer');

const measureOptions = [
  { foreground: false, title: 'Scroll activity', detail: 'Counts when X reports scrolling and pauses after a short idle period.' },
  { foreground: true, title: 'Time in X', detail: 'Counts time with X open, including time spent reading.' },
] as const;

function serviceStatus(state: TimerState) {
  if (!state.enabled) return 'Off';
  if (!state.granted) return 'Needs Android permission';
  if (!state.running) return 'Waiting for Android';
  if (state.overlayFailed) return 'Overlay unavailable';
  return state.locked ? 'X is locked' : 'Ready';
}

function formatTime(seconds: number) {
  const hours = Math.floor(seconds / 3_600);
  const minutes = Math.floor(seconds / 60) % 60;
  const remainder = String(seconds % 60).padStart(2, '0');
  return hours === 0 ? `${minutes}:${remainder}` : `${hours}:${String(minutes).padStart(2, '0')}:${remainder}`;
}

export function ScrollTimerScreen({ onBack }: { onBack: () => void }) {
  const [state, setState] = useState<TimerState>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const back = BackHandler.addEventListener('hardwareBackPress', () => {
      onBack();
      return true;
    });
    return () => back.remove();
  }, [onBack]);

  useEffect(() => {
    if (!timer) return;
    const native = timer;
    let mounted = true;

    async function refresh() {
      try {
        const snapshot = await native.getState();
        if (mounted) setState(snapshot);
      } catch {
        if (mounted) setError('Could not read the scroll timer settings.');
      }
    }

    void refresh();
    const subscription = AppState.addEventListener('change', (next) => {
      if (next === 'active') void refresh();
    });
    const poll = setInterval(() => {
      if (AppState.currentState === 'active') void refresh();
    }, 2_000);
    return () => {
      mounted = false;
      clearInterval(poll);
      subscription.remove();
    };
  }, []);

  async function update(change: () => Promise<TimerState>) {
    setBusy(true);
    setError('');
    try {
      setState(await change());
    } catch {
      setError('Could not update the scroll timer settings.');
    } finally {
      setBusy(false);
    }
  }

  async function openAccessibility() {
    setError('');
    try {
      await Linking.sendIntent('android.settings.ACCESSIBILITY_SETTINGS');
    } catch {
      setError('Open Android Settings and look for Accessibility.');
    }
  }

  return (
    <ScrollView contentContainerStyle={styles.screen}>
      <Pressable accessibilityRole="button" onPress={onBack} style={styles.back}>
        <Text style={styles.link}>‹  Follow reasons</Text>
      </Pressable>
      <Text accessibilityRole="header" style={styles.title}>Scroll timer</Text>
      <Text style={styles.detail}>
        See when you’re scrolling in X. Lock X when you want a break, then confirm when you’re ready to unlock.
      </Text>
      {error !== '' && <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text>}
      {!timer && <Text style={styles.detail}>The timer is available in the X Manager Android APK.</Text>}
      {timer && state && (
        <>
          <View style={styles.card}>
            <View style={styles.row}>
              <View style={styles.grow}>
                <Text style={styles.label}>Show timer in X</Text>
                <Text accessibilityLiveRegion="polite" style={styles.detail}>{serviceStatus(state)}</Text>
              </View>
              <Switch
                accessibilityLabel="Show timer in X"
                disabled={busy}
                onValueChange={(enabled) => void update(() => timer.setEnabled(enabled))}
                thumbColor={state.enabled ? color.green : color.card}
                trackColor={{ false: color.line, true: color.greenWash }}
                value={state.enabled}
              />
            </View>
            <Text style={styles.detail}>
              Android Accessibility lets the timer identify the active X window and receive scroll events.
              Your timer and lock stay on this phone.
            </Text>
            {state.enabled && (
              <Pressable accessibilityRole="button" onPress={() => void openAccessibility()} style={styles.back}>
                <Text style={styles.link}>Open Accessibility Settings ↗</Text>
              </Pressable>
            )}
            {state.locked && !state.running && (
              <Text style={styles.detail}>Your lock resumes when the timer service is enabled.</Text>
            )}
          </View>

          <Text accessibilityRole="header" style={styles.section}>Measure</Text>
          <View style={styles.options}>
            {measureOptions.map(({ foreground, title, detail }) => (
              <Pressable
                key={title}
                accessibilityRole="radio"
                accessibilityState={{ checked: state.foregroundMode === foreground, disabled: busy }}
                disabled={busy || state.foregroundMode === foreground}
                onPress={() => void update(() => timer.setForegroundMode(foreground))}
                style={[styles.option, state.foregroundMode === foreground && styles.selected]}
              >
                <Text style={styles.label}>{title}</Text>
                <Text style={styles.detail}>{detail}</Text>
              </Pressable>
            ))}
          </View>
          <View style={styles.row}>
            <View style={styles.grow}>
              <Text style={styles.label}>{state.foregroundMode ? 'Time in X' : 'Scroll time'}</Text>
              <Text style={styles.value}>{formatTime(state.totalSeconds)}</Text>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Reset the selected timer"
              disabled={busy || state.totalSeconds === 0}
              onPress={() => void update(() => timer.resetTime())}
              style={[styles.back, state.totalSeconds === 0 && styles.dim]}
            >
              <Text style={styles.link}>Reset time</Text>
            </Pressable>
          </View>
          {state.running && !state.foregroundMode && !state.sawScrollEvents && (
            <Text style={styles.detail}>If the timer stays paused while you scroll, try Time in X.</Text>
          )}
        </>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { paddingHorizontal: 24, paddingBottom: 32, gap: 14 },
  back: { alignSelf: 'flex-start', justifyContent: 'center', minHeight: 48 },
  link: { color: color.green, fontSize: 15, fontWeight: '700' },
  title: { color: color.ink, fontFamily: 'serif', fontSize: 39, fontWeight: '600' },
  card: { gap: 10, padding: 16, borderWidth: 1, borderColor: color.line, borderRadius: 10, backgroundColor: color.card },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  grow: { flex: 1, gap: 4 },
  label: { color: color.ink, fontSize: 16, fontWeight: '700' },
  detail: { color: color.muted, fontSize: 14, lineHeight: 21 },
  section: { color: color.ink, fontSize: 16, fontWeight: '700', marginTop: 6 },
  options: { gap: 8 },
  option: { gap: 4, padding: 12, borderWidth: 1, borderColor: color.line, borderRadius: 8, backgroundColor: color.card },
  selected: { backgroundColor: color.greenWash, borderColor: color.green },
  value: { color: color.ink, fontSize: 25, fontVariant: ['tabular-nums'] },
  dim: { opacity: 0.5 },
  error: { padding: 12, color: color.rust, backgroundColor: color.rustWash, borderRadius: 8 },
});
