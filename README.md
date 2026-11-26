# M4Y4D A! – AI Browser (Android starter project)

A native Android WebView browser starter project with a dark AI-inspired interface.

## Included
- Address/search bar (Google search fallback)
- Multiple tabs (basic tab switching and closing)
- Back, forward and reload
- Bookmarks and browsing history (stored locally)
- Private-mode toggle (clears cookies and uses no-cache WebViews)
- Download listener using Android DownloadManager
- AI chat dialog using an OpenAI-compatible Chat Completions endpoint
- Settings to add/change API endpoint, model and API key

## Build
1. Install Android Studio on a computer.
2. Open this folder as an existing Gradle project.
3. Let Gradle sync. Use JDK 17.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).

The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21 and compileSdk 35.

## Important security notes
- This is a starter project, not a production-hardened browser.
- An API key stored in an APK can be extracted by a determined user. Do not distribute a build containing a personal/private API key. For a public app, put AI requests behind your own backend and issue per-user limits/authentication.
- Private mode reduces local storage, but this is not equivalent to a forensic-grade private browser. Websites, networks, DNS providers and downloaded files may still reveal activity. Closing/restarting the app and clearing cookies/history should be tested before release.
- The app currently uses Android WebView, not the full Chromium browser engine. Some Chrome features and extensions are not included.
- Add a proper privacy policy, safe browsing protection, permission handling and security review before publishing.
