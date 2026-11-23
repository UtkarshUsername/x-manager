# X Manager

X Manager is a personal Android follow log. In X, open the post that made you follow an account, tap **Share**, and choose **X Manager** in the Android share sheet. If X opens its own sharing menu first, tap **Share via…** to reach the Android sheet. The post saves in the background, and you stay in X. A brief Android message confirms the result.

Each share records the account handle, post link, and the time you saved it. The newest saves appear first. Search by handle, with or without the `@`, and tap a row to reopen the post. Tap **Remove** beside any saved post and confirm to delete it from X Manager.

X and legacy Twitter status links are saved as clean `x.com` links. Sharing the same status again keeps the first saved time. Links without a handle, shortened links, and shares with conflicting post links show an explanation. Share a direct status link that includes the handle.

Saved posts live in the app’s SQLite database on your phone. The app has no sign-in or sync, and Android cloud backup is disabled for its data. Clearing app storage or uninstalling removes your saved posts.

## Scroll timer and lock

1. Open **Scroll timer** in X Manager and turn on **Show timer in X**.
2. Tap **Open Accessibility Settings** and enable **X Manager scroll timer** under the downloaded or installed services. When Android restricts this setting for a sideloaded APK, you can allow it from X Manager’s **App info** menu. [Android restricted settings help](https://support.google.com/android/answer/12623953)
3. Open the official X app. A floating timer and **Lock** button appear. Drag the timer text to move it out of the way. X Manager remembers where you put it. The scroll timer pauses about a second after the last reported scroll. Reading or opening Android’s share sheet keeps you in the same session. Switching to another app or turning off the screen ends it. The next session starts at zero. If X’s scroll events are unreliable on your phone, choose **Time in X** to count foreground time, including reading.
4. Tap **Lock** to block interaction with X. Home, Settings, and other apps stay available. The lock returns when you reopen X and remains until you tap **Unlock** and then **Confirm unlock**. **Keep locked** dismisses the confirmation. Turning the service off removes its overlay. When you enable it again, a saved lock resumes.

X Manager uses the focused window’s package name and the official X app’s scroll events. The settings screen shows the latest session’s time in the selected mode. The latest completed session and lock state are stored on the phone.

## Build and install the preview APK

Use Node.js 22.13 or later on the Node 22 line and an Expo account. Expo Build runs the Android toolchain in the cloud. The laptop needs no Android Studio, JDK, Android SDK, or emulator. The preview APK runs on its own once installed.

From the project root in PowerShell:

```powershell
npm ci
npx eas-cli@latest login
npx eas-cli@latest build --platform android --profile preview
```

For an update, link to the same Expo project as the installed APK and reuse its remote Android keystore. Use the same Android package name and a higher version code. When you run the build, EAS uploads the project source to compile and sign the APK. Your saved posts are created locally on the phone.

After the build finishes, open the install link or scan the QR code from the EAS build page on your Android phone. Download the APK and open it. If Android blocks the install, allow app installation from the browser or file manager you used, then install the APK. Install updates over the existing app to retain your saved posts.

See Expo’s [APK build and install guide](https://docs.expo.dev/build-reference/apk/) and [project upload guide](https://docs.expo.dev/build-reference/project-upload/).

## Verify on a phone

1. Install over the previous APK and confirm your old bookmarks still appear in X Manager.
2. Close X Manager. Share an X post from the Android share sheet, then share another while X Manager is running in the background. Confirm X stays open and Android shows **Post saved**. Open X Manager and check the handles and save times, with the latest save first.
3. Share the first post again. Confirm the app reports that it is already saved and keeps the original time.
4. Share a legacy Twitter status link, a handle-less `x.com/i/status/…` link, and an unrelated link from a browser or text app. The legacy link should save. The other two should show an explanation.
5. Close and reopen X Manager. Search for part of a handle and tap a matching row to open the post.
6. Enable the timer and make several swipes in X. Watch for **Scrolling**, then read for a few seconds and check that time stops. Drag the timer text toward each edge and check that the whole timer stays reachable. Share a post and verify the timer keeps the same session total when you return. Switch apps and come back, then repeat after turning the screen off. Verify each new session starts at zero and the timer returns to your chosen position. If the overlay stays at **Waiting for scroll**, switch to **Time in X** and check it counts while you read.
7. Tap **Lock** while in X. Try swiping and tapping the feed. Verify Home, Settings, and another app remain usable, then reopen X and check it is still locked. Tap **Unlock**, choose **Keep locked**, and try a swipe again. Then tap **Unlock** and **Confirm unlock**, and verify scrolling works.
8. While locked, turn the screen off and back on. Verify the lock returns only in X. Disable the service in Android Settings and verify the overlay disappears. Re-enable it and confirm the lock returns until you unlock it.
9. Leave X Manager on its timer screen, then share another post from X. Verify X stays visible, the timer screen is still selected when you return to X Manager, and the post appears when you open the follow log.
10. Tap **Remove** beside a saved post and cancel. Then confirm removal and check that the post stays gone after reopening X Manager. Share the same post again and check that it has a new saved time.

Use the installed APK for the share-sheet checks. Android registers the share target from the generated app manifest.

## Local checks

```powershell
npm test
npm run typecheck
npx expo install --check
npx expo-doctor
npx expo export --platform android
```

## License

X Manager is licensed under the [MIT License](LICENSE).
