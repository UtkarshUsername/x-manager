# X Manager

X Manager is an Android app for saving why you followed someone on X, tracking how long you scroll, and locking X when you want a break. [Build and install the APK](#build-and-install-on-android)

## Save a follow reason

In X, tap **Share** on a post, then **Share via…** if needed, and choose **X Manager**. The post saves in the background while you stay in X.

In **Follow reasons**, search by account handle, tap a saved post to reopen it, or tap **Remove** and confirm. Direct X and legacy Twitter post links with a handle are supported. Saving the same post twice keeps its original save time.

## Scroll timer and lock

In X Manager, open **Scroll timer**, turn on **Show timer in X**, and use **Open Accessibility Settings** to enable **X Manager scroll timer**. In the official X app, drag the timer by its text to move it. X Manager remembers its position.

- **Scroll activity** counts time when X reports scrolling. **Time in X** also counts reading time and works when scroll events are unreliable.
- The share sheet pauses the current session. Switching apps or turning off the screen ends it. The next session starts at zero.
- Tap **Lock** to block taps and swipes in X while keeping other apps usable. The lock persists when you leave X. To end it, tap **Unlock**, then **Confirm unlock**.

Saved posts and timer data stay on your phone. Clearing app storage or uninstalling erases them. Only the timer and lock require Accessibility.

## Build and install on Android

Use Node.js 22.13 or later on the Node 22 line and an Expo account. Run from the project root.

```sh
npm ci
npx eas-cli@latest login
npx eas-cli@latest init
npx eas-cli@latest build --platform android --profile preview
```

EAS uploads the checkout and builds the APK in the cloud. On a first build, create an Expo project and generate a signing key when prompted. When the build finishes, scan the QR code on its Expo page to install the APK. [Expo’s APK build guide](https://docs.expo.dev/build-reference/apk/)

For updates, keep the Android package name and signing key, increase `expo.android.versionCode` in `app.json`, and install over the previous APK to retain saved posts. If EAS manages your signing key, use the same Expo project.

<details>
<summary>Android installation and permission prompts</summary>

- **Play Protect blocks the browser install.** For an APK you built yourself, download it to your computer. Enable USB debugging and authorize the computer on your phone, then use Android Platform-Tools to run `adb install -r ./x-manager.apk` with your APK path. [Android ADB guide](https://developer.android.com/tools/adb)
- **Accessibility shows “Restricted setting.”** In X Manager’s **App info**, choose **⋮ → Allow restricted settings**. Return to **Accessibility** and enable the timer. [Android restricted settings help](https://support.google.com/android/answer/12623953)

</details>

## Verify on a phone

<details>
<summary>Android regression checklist</summary>

- **Update.** Install over the previous APK and check that saved posts survive.
- **Sharing.** Share with X Manager closed and with its timer screen in the background. Check that X stays visible and the timer screen stays selected. Check that legacy links save, duplicates keep their original save time, and unsupported links show an explanation.
- **Saved posts.** Check newest-first ordering, search, and reopening a post. Cancel a removal, then confirm one and check that it persists after reopening the app. Share the post again to check its new save time.
- **Timer.** Check that scroll time pauses while reading and both modes pause in the share sheet. Drag to the edges. After switching apps or turning the screen off, check that the timer resets and stays in position.
- **Lock.** Check that taps and swipes are blocked only in X. Leave and reopen X, turn the screen off and back on, and toggle the service to verify the lock resumes. Try **Keep locked** and **Confirm unlock**.

</details>

## License

X Manager is licensed under the [MIT License](LICENSE).
