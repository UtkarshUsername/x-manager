# X Manager

X Manager is an Android app for saving why you followed someone on X, tracking how long you scroll, and locking X when you want a break.

## Save a follow reason

1. In X, open the post that made you follow an account. Tap **Share**, then **Share via…** if X opens its own sharing menu first.
2. Choose **X Manager** in the Android share sheet. The account handle, post link, and save time are recorded in the background while you stay in X. A brief message confirms the result.
3. Open **Follow reasons** in X Manager. Saved posts appear newest first. Search by handle, with or without the `@`, and tap a post to reopen it. To delete a bookmark from X Manager, tap **Remove** and confirm. You can share it again to record a new save time.

Direct `x.com` and legacy `twitter.com` status URLs with a handle are supported. X Manager saves a clean `x.com` link and keeps the first save time if you share the same post twice. Shares with shortened links, missing handles, or conflicting post links show an explanation.

## Scroll timer and lock

1. In X Manager, open **Scroll timer** and turn on **Show timer in X**.
2. Tap **Open Accessibility Settings** and enable **X Manager scroll timer** under downloaded or installed services.
3. Open the official X app. Drag the floating timer by its text to move it. X Manager remembers the position.

The timer has two measurement modes.

- **Scroll activity** counts time based on scroll events from X and pauses about a second after the last event.
- **Time in X** also counts reading time while X is in the foreground. Use it if your phone does not report scroll events reliably.

Opening the Android share sheet pauses the timer within the same session. Switching apps or turning off the screen ends the session. The next session starts at zero, and the **Scroll timer** screen shows the latest session total.

Tap **Lock** to block taps and swipes in X. Home, Settings, and other apps stay available. The lock returns when you reopen X. Tap **Unlock**, then **Confirm unlock** to end it, or **Keep locked** to dismiss the prompt. Turning the timer service off removes the overlay. A saved lock resumes when you enable the service again.

## Data and permissions

Saved posts, the timer position, the latest completed session, and the lock state stay on your phone. X Manager has no sign-in or sync. Android cloud backup is disabled. Clearing app storage or uninstalling erases the saved data.

The optional timer uses Android Accessibility to read the focused window’s package name, receive X’s scroll events, and display the overlay. Saving shared posts works with the timer off.

## Build and install on Android

The Android APK includes the share target and Accessibility service. Test these features with the installed APK.

Use Node.js 22.13 or later on the Node 22 line and an Expo account. Expo Build runs the Android toolchain in the cloud, so you can build without installing Android Studio, a JDK, the Android SDK, or an emulator.

Run these commands from the project root.

```sh
npm ci
npx eas-cli@latest login
npx eas-cli@latest init
npx eas-cli@latest build --platform android --profile preview
```

On a first build, create an Expo project under your account and let EAS generate a keystore if you do not have one. EAS uploads the source from your checkout to compile and sign the APK. After the build finishes, open the build page, scan its QR code on your phone, and install the APK. [Expo’s APK build guide](https://docs.expo.dev/build-reference/apk/) · [How EAS uploads projects](https://docs.expo.dev/build-reference/project-upload/)

To update an existing install and keep your saved posts, build with the same Android package name and signing key, and increase `expo.android.versionCode` in `app.json`. If EAS manages your key, use the same Expo project so it can reuse the keystore. Install the new APK over the existing one.

### Android installation and permission prompts

- **Play Protect blocks the browser install.** Play Protect can block browser installs of apps that declare an Accessibility service. For an APK you built from this source, download it to your computer and install it over USB. Use Android Platform-Tools, enable USB debugging, approve the computer on your phone, and run `adb install -r ./x-manager.apk` with the path to your APK. [Play Protect guidance](https://developers.google.com/android/play-protect/warning-dev-guidance#app_blocked_to_protect_your_device) · [Android ADB guide](https://developer.android.com/tools/adb)
- **Accessibility shows “Restricted setting.”** Open X Manager’s **App info** in Android Settings, tap **⋮ → Allow restricted settings**, and confirm if prompted. Then return to **Accessibility** and enable the timer service. [Android restricted settings help](https://support.google.com/android/answer/12623953)

## Verify on a phone

- Install over the previous APK and check that saved posts are still there.
- Share posts with X Manager closed and with its timer screen open in the background. Check that X stays visible, the saves appear newest first, and the timer screen stays selected. Share a duplicate and check that it keeps the original save time.
- Try a legacy Twitter link, a handle-less `x.com/i/status/…` link, and an unrelated link. Check that the legacy link saves and the other two show an explanation.
- Search by handle and reopen a post. Cancel a removal, then confirm it. Check that the post stays gone after reopening X Manager and that sharing it again gives it a new save time.
- Scroll, pause to read, and open the share sheet. Check that the timer pauses and resumes in the same session. Drag it toward each edge. Switch apps or turn off the screen, then return to X and check for a fresh session with the timer at your chosen position. Try **Time in X** and verify that it also counts while you read.
- Lock X and try tapping and swiping. Use Home, Settings, and another app, then return to X and check that it stays locked. Repeat after turning off the screen and after disabling and re-enabling the service. Check that **Keep locked** preserves the lock and **Confirm unlock** restores interaction.

## Local checks

The app uses Expo, React Native, and TypeScript, with Kotlin modules for the share target, on-device SQLite storage, and the Accessibility overlay.

```sh
npm test
npm run typecheck
npx expo install --check
npx expo-doctor
npx expo export --platform android
```

## License

X Manager is licensed under the [MIT License](LICENSE).
