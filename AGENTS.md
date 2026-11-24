# X Manager

## Conventions

- Prefer simple, readable, fast code. Remove obsolete paths when replacing them. Omit em dashes.
- Make focused commits at appropriate points. Use Conventional Commit subjects phrased as commands. Create a new commit for corrections unless the user explicitly asks to amend.

## Android behavior

- Sharing and storage changes must preserve background saving when the React Native UI is closed and keep existing bookmarks readable after an update. Treat X post IDs as strings.
- Timer and lock changes must keep Accessibility opt-in, use window ownership and X scroll events without collecting feed content, and keep Home, Settings, and other apps usable.
- For changes to user-facing behavior, keep the corresponding instructions in [README.md](README.md) accurate.

## Verification and builds

- Run the test and typecheck scripts in `package.json` for code changes. Verify changes to sharing, the timer, or the lock on an installed Android APK using the relevant [phone checks](README.md#verify-on-a-phone). Report any device checks you could not run.
- For requested APK builds and updates, follow the [EAS preview instructions](README.md#build-and-install-on-android). Keep the Android build toolchain in EAS. Upload source to EAS only on an explicit build request.
- Keep private Expo project configuration, signing credentials, and personal data out of Git.
