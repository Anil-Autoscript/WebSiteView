# 📱 WebViewer Pro — Android App

A beautiful, feature-rich Android WebView application that lets you save and browse any HTML/web URL in a polished native app experience.

---

## ✨ Features

| Feature | Details |
|---------|---------|
| 🌐 Full WebView | JavaScript, DOM storage, geolocation, file access |
| 🔗 URL Manager | Save, edit, reorder, delete URLs — persisted locally |
| 🏠 Home URL | Configurable home page |
| 🔄 Swipe to Refresh | Pull down to reload the current page |
| 🔍 Smart URL Bar | Auto-detects URLs vs. search queries |
| 🌙 Dark Theme | Deep navy/purple dark UI throughout |
| ⚡ Splash Screen | Animated logo on launch |
| ⚙️ Settings | JS toggle, desktop mode, cache control |
| 📤 Share | Share current URL with any app |
| 🤖 GitHub Actions | Auto-build debug + release APK on every push |

---

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO
```

### 2. Add Your URLs
Open the app → tap the **link icon** (top right) → tap **+** to add any URL.

Or set a default home URL in **Settings**.

### 3. Build Locally
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```
APK output: `app/build/outputs/apk/`

---

## 🤖 GitHub Actions CI/CD

The workflow at `.github/workflows/build.yml` automatically:

| Trigger | Action |
|---------|--------|
| Push to `main` / `master` | Builds debug + release APK, uploads as artifacts |
| Pull Request | Builds debug APK to verify it compiles |
| Tag `v*` (e.g. `v1.0.0`) | Builds both APKs + creates a **GitHub Release** with download links |
| Manual trigger | Choose debug or release from the Actions tab |

### Creating a Release
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions will automatically build and publish the release with downloadable APKs.

### Downloading APKs from Actions
1. Go to your repo → **Actions** tab
2. Click the latest successful workflow run
3. Scroll to **Artifacts** → download the APK zip
4. Extract and install the `.apk` on your Android device

---

## 📂 Project Structure

```
AndroidWebApp/
├── .github/
│   └── workflows/
│       └── build.yml          # CI/CD pipeline
├── app/
│   └── src/main/
│       ├── java/com/webviewer/app/
│       │   ├── SplashActivity.java      # Animated splash screen
│       │   ├── MainActivity.java        # Main WebView browser
│       │   ├── UrlManagerActivity.java  # URL list manager
│       │   └── SettingsActivity.java    # App settings
│       └── res/
│           ├── layout/        # XML layouts
│           ├── drawable/      # Icons & shapes
│           ├── values/        # Colors, strings, themes
│           ├── anim/          # Fade animations
│           ├── menu/          # Toolbar menu
│           └── xml/           # Network config, file paths
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradlew
```

---

## 📱 Installation on Device

1. **Enable Unknown Sources**: Settings → Security → Install Unknown Apps
2. Download the APK from GitHub Actions artifacts or Releases
3. Open the APK file on your Android device
4. Tap **Install**

> Minimum Android version: **5.0 (API 21)**  
> Target Android version: **14 (API 34)**

---

## 🎨 Customisation

### Change Default Home URL
Edit `MainActivity.java`:
```java
private static final String DEFAULT_URL = "https://your-site.com";
```

### Change App Name
Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">My Custom Browser</string>
```

### Change Theme Colors
Edit `app/src/main/res/values/colors.xml`:
```xml
<color name="accent_primary">#6C63FF</color>   <!-- Main purple -->
<color name="accent_secondary">#00D4AA</color>  <!-- Teal accent -->
<color name="bg_primary">#0F1117</color>        <!-- Background -->
```

### Change App Package
In `app/build.gradle` change:
```groovy
applicationId "com.webviewer.app"
```
Then refactor the package in all Java files.

---

## 🔒 Permissions Used

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Load web pages |
| `ACCESS_NETWORK_STATE` | Check connectivity |
| `CAMERA` | Allow camera access in web pages |
| `READ/WRITE_EXTERNAL_STORAGE` | File downloads |

---

## 🛠 Tech Stack

- **Language**: Java
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Libraries**: AndroidX, Material Components, WebKit, SwipeRefreshLayout, RecyclerView
- **Build**: Gradle 8.4 + Android Gradle Plugin 8.2.2
- **CI/CD**: GitHub Actions

---

## 📄 License

MIT License — free to use and modify.
