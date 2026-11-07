# Running The Project In Android Studio

Written for this machine: Android Studio is at `C:\Program Files\Android\Android Studio`,
the SDK is at `C:\Users\RASHWELL\AppData\Local\Android\Sdk`, and the project lives at
`C:\Users\RASHWELL\Desktop\FitnessTracker`.

---

## Step 1 — Open the project

1. Launch **Android Studio** (Start menu → Android Studio).
2. On the welcome window click **Open** (not "New Project").
3. Navigate to `C:\Users\RASHWELL\Desktop\FitnessTracker` and click **OK**.

Choose the folder that contains `settings.gradle` — that folder **is** the project.
Do not open the `app` subfolder.

4. If a dialog says *"Gradle wrapper / Gradle JDK not found"* or asks to trust the project,
   click **Trust Project** and let it use the bundled JDK.
5. Wait for **"Gradle sync finished"** in the status bar (first sync downloads
   dependencies, so give it a few minutes).

> `local.properties` already points at your SDK. If Android Studio complains the SDK is
> missing, open **File → Project Structure → SDK Location** and confirm the path above.

---

## Step 2 — Wait for indexing to finish

The first time you open a project, Android Studio indexes it. The two Java files and the
three layouts will show red text until indexing and sync complete. This is normal.

---

## Step 3 — Create a device (once only)

**Tools → Device Manager → Create Virtual Device** (or the phone icon in the toolbar):

1. Pick **Pixel 7** (or any phone) → **Next**.
2. Pick a system image, e.g. **API 35 (Android 15)** — download it if there is a
   download arrow next to it → **Next** → **Finish**.
3. Click the ▶ play button on the device to boot the emulator.

Physical phone instead: enable **Developer options → USB debugging**, plug it in, then
accept the "Allow USB debugging" prompt. It appears in the device dropdown.

---

## Step 4 — Run the app

1. In the toolbar dropdown next to the ▶ button, select **app**.
2. Select your device in the dropdown to its right.
3. Click the green ▶ **Run 'app'** button (or press `Shift + F10`).

Android Studio builds, installs and launches the app. The first build takes a couple of
minutes; later runs are seconds.

**If the run configuration list is empty:** open `MainActivity.java`, right-click in the
code, choose **Run 'MainActivity'**.

---

## Step 5 — Check that it works

1. **Main screen** shows `0` workouts, `0 kcal`, and "No workouts logged yet."
2. Tap **Log Workout** → fill in type `Running`, duration `30`, calories `250`,
   intensity `High` → tap **Save Workout**.
   - The screen closes and a "Workout saved" toast appears.
   - The list now has one row: `Running / 30 min - 250 kcal - High / <today's date>`.
   - The summary reads `1` and `250 kcal`.
3. **Validation:** tap **Log Workout** and press **Save** with everything empty — each
   field turns red with a message. Type `abc` in the duration field and save — a toast
   says duration and calories must be numbers. Try `0` — "must be greater than 0".
4. **Delete:** tap **Delete** on a row → a dialog asks for confirmation → **Cancel** keeps
   it, **Delete** removes it and the totals drop back down.
5. **Persistence:** close the app completely (swipe it from recents) and open it again —
   the workouts are still there, because they live in SQLite, not in memory.
6. **Going back:** open Log Workout, type something, then tap the **up arrow** in the app
   bar — you return to Main with nothing saved. The system Back button does the same.

---

## Step 6 — Look at the database (good for the defence)

**View → Tool Windows → App Inspection → Database Inspector**.

1. With the app running, select the process `com.example.fitnesstracker`.
2. Open `FitnessTrackerDB` → `Workouts`.
3. You will see the live rows. Live-update is on by default, so adding a workout in the
   emulator makes a row appear here immediately.

Screenshot this — it is the fastest way to prove the SQLite part works.

---

## Useful commands (optional)

Run these in the project folder in a terminal, if you prefer the command line:

```bat
gradlew.bat assembleDebug      :: build the debug APK
gradlew.bat installDebug       :: build + install on the connected device
gradlew.bat clean              :: delete build outputs
```

The APK is written to `app\build\outputs\apk\debug\app-debug.apk`.
You can copy that file to a phone and install it directly.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| First field on Log Workout is hidden, or the summary card sits under the app bar | Edge-to-edge drawing. `themes.xml` already sets `android:windowOptOutEdgeToEdgeEnforcement` for this; if you changed `targetSdk` to 36 the attribute is ignored and the layouts need window-inset padding instead |
| Pressing Save with an empty form appears to do nothing | Validation errors are set on the `TextInputLayout` wrapper, not the `EditText`. If you move the error call to `EditText.setError(...)`, nothing renders inside an outlined field — that was a real bug here, fixed |
| Workouts from the older `AndroidStudioProjects\FitnessTracker` build show up here | Both projects use `applicationId com.example.fitnesstracker`, so they share one app — and one `FitnessTrackerDB` file. Uninstall the app from the device (Settings → Apps, or `adb uninstall com.example.fitnesstracker`) before installing the other build to start clean |
| Emulator shows no keyboard when typing | `adb shell ime enable com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME` — the IME is a per-AVD setting and can be left disabled while testing text entry from `adb` |
| "SDK location not found" | Check `local.properties` contains `sdk.dir=C:/Users/RASHWELL/AppData/Local/Android/Sdk`, or set it in **Project Structure → SDK Location**. |
| Gradle sync fails with a network error | **File → Settings → Build, Execution, Deployment → Gradle** → uncheck *Offline mode*, then **File → Sync Project with Gradle Files**. |
| "Unsupported class file major version" / JDK error | **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** → choose the **jbr-21** (bundled) or JDK 21 entry, then sync again. Must be JDK 17 or newer. |
| Red `R.layout....` everywhere | The project has not synced yet, or there is an XML error in `res/layout`. Run **Build → Make Project** and read the Build output at the bottom. |
| App installs but shows a blank screen | Confirm `MainActivity` is the launcher activity in `AndroidManifest.xml` and that the theme in the manifest is `@style/Theme.FitnessTracker`. |
| Emulator will not start | Use a phone instead over USB, or create a device with a system image you have already downloaded. |
| Delete does nothing | The confirm dialog must be answered with **Delete** (not **Cancel**) — that is the required confirmation step. |

---

## What to say during the defence

- **Architecture:** two activities, one adapter, one model class, one `SQLiteOpenHelper`.
- **Where the data lives:** `FitnessTrackerDB` → table `Workouts`, six columns exactly as
  specified, `id` is `AUTOINCREMENT`.
- **Adding:** `LogWorkoutActivity` validates (empty → number → greater than zero), builds a
  `Workout`, then `addWorkout()` runs `db.insert(...)`.
- **Displaying:** `getAllWorkouts()` runs `db.query(...)` ordered by `id DESC`;
  the adapter binds each row; the summary sums the calories in one loop.
- **Deleting:** row button → `AlertDialog` confirmation → `db.delete(...)` with
  `WHERE id = ?` → list reloads.
- **Refreshing:** `onResume()` reloads, so coming back from the form is always up to date.
- **Going back:** the up arrow in the app bar is drawn because the manifest sets
  `parentActivityName`, and `onOptionsItemSelected` handles `android.R.id.home` with
  `finish()`. The system Back button closes the screen by default; neither saves anything.
- **Material Design:** Material 3 theme, `MaterialCardView` for cards and rows,
  `MaterialButton`, `TextInputLayout` with outlined boxes, `colorError` on the delete
  button, 16dp screen padding / 12dp field spacing / 8dp row spacing.
