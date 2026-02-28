# 📱 HTML Combiner — Android App

Combine multiple JavaScript and CSS files into a single self-contained HTML file, with a built-in **localhost HTTP server** to serve and preview your project in-app.

---

## ✨ Features

| Feature | Description |
|---|---|
| **File Picker** | Pick multiple `.js`, `.css`, `.html` files from storage |
| **Local Server** | NanoHTTPD server on `http://localhost:8080` serves all added files by name |
| **In-App Preview** | WebView shows the server's index page and any HTML file |
| **Inline Combine** | Embeds all CSS/JS directly into one `<style>` + `<script>` block |
| **Link Combine** | Generates HTML that links to `localhost:8080/filename` (server mode) |
| **Minify** | Basic CSS and JS whitespace/comment stripping |
| **Drag to Reorder** | Long-press to reorder files (controls load order) |
| **Swipe to Remove** | Swipe left to delete a file from the list |
| **Toggle files** | Enable/disable individual files without removing them |
| **Save HTML** | Export combined HTML via Android's native file picker |

---

## 🏗 Building the APK

### Option A — GitHub Actions (Recommended)

1. Push this project to a GitHub repo
2. Go to **Actions** tab → **Build APK** workflow → **Run workflow**
3. Download the APK from **Artifacts** once the run completes

### Option B — Termux (on Android)

```bash
# 1. Install dependencies
pkg update && pkg upgrade -y
pkg install -y git openjdk-17 gradle

# 2. Clone your repo
git clone https://github.com/YOUR_USER/YOUR_REPO.git
cd YOUR_REPO

# 3. Download Android SDK command-line tools
mkdir -p $HOME/android-sdk/cmdline-tools
cd $HOME/android-sdk/cmdline-tools
curl -O https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# 4. Set environment variables
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 5. Accept licenses and install required SDK components
yes | sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0"

# 6. Go back to project and build
cd ~/YOUR_REPO
chmod +x gradlew

# Download gradle wrapper jar
mkdir -p gradle/wrapper
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.1.1/gradle/wrapper/gradle-wrapper.jar

# Build!
./gradlew assembleDebug

# APK location
ls app/build/outputs/apk/debug/
```

### Option C — Local machine (Linux/Mac/Windows)

Requires Android Studio or Android SDK + JDK 17.

```bash
# Clone, then:
chmod +x gradlew
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Project Structure

```
HTMLCombiner/
├── .github/workflows/build.yml     ← GitHub Actions CI
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/htmlcombiner/
│   │   ├── MainActivity.java       ← Main UI controller
│   │   ├── FileItem.java           ← Data model
│   │   ├── FileAdapter.java        ← RecyclerView adapter
│   │   ├── LocalServer.java        ← NanoHTTPD server
│   │   ├── FileCombiner.java       ← Core combine logic
│   │   └── ServerService.java      ← Foreground service
│   └── res/
│       ├── layout/activity_main.xml
│       ├── layout/item_file.xml
│       ├── layout/dialog_combine_options.xml
│       └── values/{colors,styles,strings}.xml
├── app/build.gradle
├── build.gradle
└── settings.gradle
```

---

## 🔧 How It Works

### Local Server Mode
1. Add your `.js`, `.css`, and `.html` files
2. Tap **🖥 Start Local Server**
3. All files are served at `http://localhost:8080/<filename>`
4. Tap **🌐 Preview** to open in the in-app WebView
5. Your HTML can reference CSS/JS by just their filename (e.g. `href="styles.css"`)

### Combine Mode
1. Add files → tap **⚡ Combine**
2. Choose:
   - **Inline** → All CSS inlined as `<style>`, all JS inlined as `<script>` — produces one fully self-contained `.html` file
   - **Linked** → HTML references `localhost:8080/file.css` etc. (server must run)
3. Enable **Minify** to strip comments and whitespace
4. Preview appears in the WebView
5. Tap **💾 Save HTML** to export

---

## 📦 Dependencies

- [NanoHTTPD 2.3.1](https://github.com/NanoHttpd/nanohttpd) — embedded HTTP server
- AndroidX AppCompat, RecyclerView, Material Components
