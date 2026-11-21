# X Manager

X Manager is a personal Android follow log. In X, open the post that made you follow an account, tap **Share**, and choose **X Manager** in the Android share sheet. If X opens its own sharing menu first, tap **Share via…** to reach the Android sheet.

Each share records the account handle, post link, and the time you saved it. The newest saves appear first. Search by handle, with or without the `@`, and tap a row to reopen the post.

X and legacy Twitter status links are saved as clean `x.com` links. Sharing the same status again keeps the first saved time. Links without a handle, shortened links, and shares with conflicting post links show an explanation. Share a direct status link that includes the handle.

Saved posts live in the app’s SQLite database on your phone. The app has no sign-in or sync, and Android cloud backup is disabled for its data. Clearing app storage or uninstalling removes your saved posts.

## Build and install the preview APK

Use Node.js 22.13 or later on the Node 22 line and an Expo account. Expo Build runs the Android toolchain in the cloud. The laptop needs no Android Studio, JDK, Android SDK, or emulator. The preview APK runs on its own once installed.

From the project root in PowerShell:

```powershell
npm ci
npx eas-cli@latest login
npx eas-cli@latest build --platform android --profile preview
```

The first build may prompt you to create or link an Expo project and generate an Android signing keystore. When you run the build, EAS uploads the project source to compile and sign the APK. Your saved posts are created locally on the phone.

After the build finishes, open the install link or scan the QR code from the EAS build page on your Android phone. Download the APK and open it. If Android blocks the install, allow app installation from the browser or file manager you used, then install the APK. Keep the same Android package name and signing key for future APKs so they can update the installed app and retain your records.

See Expo’s [APK build and install guide](https://docs.expo.dev/build-reference/apk/) and [project upload guide](https://docs.expo.dev/build-reference/project-upload/).

## Verify on a phone

1. Start with X Manager closed. Share an X post using Android’s share sheet and confirm that X Manager opens and shows the handle and save time.
2. Go back to X and share a different post while X Manager is still running. Confirm both entries appear, with the latest save first.
3. Share the first post again. Confirm the app reports that it is already saved and keeps the original time.
4. Share a legacy Twitter status link, a handle-less `x.com/i/status/…` link, and an unrelated link from a browser or text app. The legacy link should save. The other two should show an explanation.
5. Close and reopen X Manager. Search for part of a handle and tap a matching row to open the post.

Use the installed APK for the share-sheet checks. Android registers the share target from the generated app manifest.

## Local checks

```powershell
npm test
npm run typecheck
npx expo install --check
npx expo-doctor
npx expo export --platform android
```
