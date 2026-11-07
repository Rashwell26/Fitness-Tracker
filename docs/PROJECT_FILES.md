# Project Files Explained, One File At A Time

Every file in this project is listed below, in the order you meet it. Nothing here is
optional decoration — if a file is listed, the app needs it.

---

## 1. Build files (how the project is compiled)

### `settings.gradle`
Tells Gradle which modules exist (`include ':app'`) and where to download libraries
from (`google()` and `mavenCentral()`). Set once, then forget it.

### `build.gradle` (project root)
Declares the Android Gradle Plugin version (`8.7.3`) but does **not** apply it here
(`apply false`). It is applied inside `app/build.gradle` instead.

### `gradle.properties`
Two important switches:
- `android.useAndroidX=true` — required, because the app uses AndroidX libraries.
- `android.nonTransitiveRClass=true` — each module gets its own `R` class.
- `org.gradle.jvmargs` — how much memory the build gets.

### `local.properties`
Holds one line: `sdk.dir=...` — the location of the Android SDK on **this** computer.
Android Studio rewrites it automatically, which is why it is git-ignored.

### `gradlew` / `gradlew.bat` / `gradle/wrapper/`
The Gradle **wrapper**: a small jar plus scripts that download and run the exact Gradle
version the project expects (8.9). You never need Gradle installed separately. Use
`gradlew.bat` on Windows, `./gradlew` on macOS/Linux.

### `app/build.gradle`
The app module's configuration:
- `namespace 'com.example.fitnesstracker'` — the package name for the generated `R` class.
- `compileSdk 35` — compile against Android 15 APIs.
- `minSdk 26` — runs on Android 8.0 and newer.
- `targetSdk 35` — behaviour matches Android 15.
- `compileOptions` — Java 17 source/target.
- `dependencies` — only three libraries:
  - `appcompat` → `AppCompatActivity`, `AlertDialog`, backward-compatible widgets
  - `material` → `MaterialButton`, `MaterialCardView`, `TextInputLayout`, the Material 3 theme
  - `recyclerview` → `RecyclerView`

### `app/proguard-rules.pro`
Empty. Release builds use the default Android optimiser rules; this app needs no custom
keep rules because nothing uses reflection.

---

## 2. `app/src/main/AndroidManifest.xml`

The app's table of contents. It says:
- which application theme to use (`@style/Theme.FitnessTracker`)
- which icon and label to show
- **`MainActivity`** is exported and has the `MAIN` / `LAUNCHER` intent filter, so it is
  the screen that opens when the icon is tapped
- **`LogWorkoutActivity`** is `exported="false"` (only this app can open it),
  has its own label used as the screen title, and declares `MainActivity` as its parent

---

## 3. Java classes — `app/src/main/java/com/example/fitnesstracker/`

Five files. That is the whole program.

### `Workout.java` — the model
A plain data holder with six fields that match the database columns exactly:
`id`, `workoutType`, `duration`, `caloriesBurned`, `intensity`, `date`.
One constructor, six getters, no logic. Its job is to carry one row between the
database and the UI.

### `WorkoutDatabaseHelper.java` — the database layer
Extends `SQLiteOpenHelper`, so Android calls it at the right moments.

- `DATABASE_NAME = "FitnessTrackerDB"`, `DATABASE_VERSION = 1` (as the task requires).
- `onCreate()` runs **once**, the first time the database is created, and executes:

  ```sql
  CREATE TABLE Workouts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      workoutType TEXT,
      duration INTEGER,
      caloriesBurned INTEGER,
      intensity TEXT,
      date TEXT
  )
  ```

- `onUpgrade()` drops and recreates the table. Only needed if the version number is ever
  increased.
- `addWorkout(Workout)` — builds `ContentValues`, calls `db.insert(...)`, returns the new
  row id, or `-1` if the insert failed.
- `getAllWorkouts()` — calls `db.query(...)` ordered by `id DESC` (newest first) and turns
  each cursor row into a `Workout` object in an `ArrayList`.
- `deleteWorkout(long id)` — calls `db.delete(...)` with `WHERE id = ?` and returns `true`
  if a row was removed.

Exception handling: every method wraps its SQLite call in `try` / `catch`, and the cursor
is closed in a `finally` block. A database problem returns an empty list or `-1` instead
of crashing the app.

### `MainActivity.java` — the landing screen
- Inflates `activity_main.xml`.
- Creates the helper: `new WorkoutDatabaseHelper(this)`.
- Sets up the `RecyclerView` with a `LinearLayoutManager` and a `WorkoutAdapter`.
- Wires **Log Workout** to `startActivity(new Intent(this, LogWorkoutActivity.class))`.
- `onResume()` → `loadWorkouts()`. Because Android calls `onResume()` every time you
  return to a screen, the list refreshes automatically after saving.
- `loadWorkouts()` — reads the list, hands it to the adapter, loops once to sum
  `caloriesBurned`, updates the two summary `TextView`s, and shows or hides the
  "no workouts yet" message.
- `onDeleteClick(Workout)` — shows an `AlertDialog`; only the **Delete** button performs
  the delete, and then the list is reloaded. **Cancel** does nothing.

### `LogWorkoutActivity.java` — the form screen
- Fills the `Spinner` from the `intensity_levels` string array in `strings.xml`
  (`ArrayAdapter.createFromResource`).
- `saveWorkout()` runs the whole flow:
  1. **Empty check** — each empty field gets an inline error via
     `TextInputLayout.setError(...)`, the wrapper around the field. (Calling
     `EditText.setError(...)` inside a `TextInputLayout` draws nothing on screen — the
     wrapper owns the error slot, so the message must be set on the wrapper.)
  2. **Number check** — `Integer.parseInt` inside `try/catch (NumberFormatException)`
     shows a toast if the user typed something that is not a number.
  3. **Range check** — duration and calories must be greater than 0.
  4. **Build the record** — the date comes from
     `new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())`, so it is stored as text.
  5. **Insert** — calls `addWorkout(...)`; if it returns `-1`, a toast reports the failure
     and the screen stays open.
  6. **Success** — toast "Workout saved", then `finish()` returns to `MainActivity`.
- **Going back** — the app bar's up arrow and the system Back button both leave this
  screen without saving. AppCompat draws the arrow because the manifest declares
  `android:parentActivityName=".MainActivity"`, and `onOptionsItemSelected` turns the
  arrow's `android.R.id.home` click into `finish()`. The system Back button needs no code:
  it already closes the activity by default.

### `WorkoutAdapter.java` — the list renderer
Bridges the `List<Workout>` and the `RecyclerView`.
- `setWorkouts(List)` replaces the data and calls `notifyDataSetChanged()`.
- `onCreateViewHolder` inflates `item_workout.xml`.
- `onBindViewHolder` writes the type, a formatted details line
  (`"30 min - 250 kcal - High"` built from `R.string.workout_details`), the date, and
  attaches the delete click listener.
- `WorkoutViewHolder` caches the four child views so `findViewById` runs once per row.
- The `OnDeleteClickListener` interface lets the adapter stay dumb: it reports the click,
  `MainActivity` decides what to do about it.

---

## 4. Layouts — `app/src/main/res/layout/`

### `activity_main.xml`
Vertical `LinearLayout`, 16dp padding:
1. `MaterialCardView` → progress overview: "Workouts logged" + `@id/totalWorkoutsText`
   on the left, "Total calories burned" + `@id/totalCaloriesText` on the right.
2. `MaterialButton` `@id/logWorkoutButton`.
3. "Recent workouts" heading.
4. `FrameLayout` containing the `RecyclerView` (`@id/workoutsRecyclerView`) and the
   centered "no workouts" `TextView` (`@id/emptyText`, `visibility="gone"` until needed).

### `activity_log_workout.xml`
`ScrollView` (so the form still works on small screens) wrapping three outlined
`TextInputLayout` fields + `MaterialButton`:
- `@id/typeInput` — text, capitalises words
- `@id/durationInput` — `inputType="number"`
- `@id/caloriesInput` — `inputType="number"`
- `@id/intensitySpinner` — plain `Spinner`, as the task requires
- `@id/saveButton` — full-width `MaterialButton`

### `item_workout.xml`
One row: `MaterialCardView` → type text + text-style **Delete** button on the first line,
details line, then date.

---

## 5. Values — `app/src/main/res/values/`

### `strings.xml`
Every user-visible word in the app, including:
- the six validation messages
- the `intensity_levels` string array (`Low`, `Medium`, `High`) that feeds the Spinner
- format strings such as `total_calories_value` (`%1$d kcal`) and `workout_details`
  (`%1$d min - %2$d kcal - %3$s`)

Keeping text here (instead of hard-coded in Java) is what makes the layout previews and
the translations work.

### `colors.xml`
Five colours: two greens, a dark green, a grey, and white. Everything else comes from the
Material 3 defaults.

### `themes.xml`
One theme, `Theme.FitnessTracker`, parent `Theme.Material3.DayNight` (so dark mode is free).
It overrides `colorPrimary`, `colorOnPrimary`, `colorPrimaryContainer`,
`colorOnPrimaryContainer`, `colorSecondary`, `colorOnSecondary`. Because the theme is
Material 3, the buttons, cards, text fields, dialogs and the app bar all inherit the same
look.

It also sets `android:windowOptOutEdgeToEdgeEnforcement="true"` (with
`tools:targetApi="35"`). **`targetSdk 35` makes Android 15+ draw the app edge-to-edge:** the
activity content is laid out from `y = 0` and the app bar is painted *over* it, which hid
the first field of the Log Workout screen behind the app bar. The opt-out restores the
pre-Android-15 behaviour — status bar, then app bar, then content — with one attribute and
no code. (The attribute is ignored by apps targeting API 36 and above; at that point the
fix is `enableEdgeToEdge()` plus window-inset padding on the root layouts.)

Finally `android:statusBarColor` is set to the darker green and
`android:windowLightStatusBar` to `false`, so the status bar matches the app bar and the
icons stay white.

### `drawable/ic_launcher.xml`
A tiny vector: green square with a white dumbbell. Referenced from the manifest as both
`android:icon` and `android:roundIcon`, so no PNG mipmap set is needed.

---

## Summary of the architecture

```
activity_main.xml ──► MainActivity ──► WorkoutAdapter ──► item_workout.xml
                            │                  │
                            │                  └── onDeleteClick → AlertDialog → delete
                            │
                            └──► WorkoutDatabaseHelper ──► FitnessTrackerDB / Workouts
                                        ▲
activity_log_workout.xml ──► LogWorkoutActivity ──► addWorkout(...)
```

Three layers, no more:
1. **UI** — two activities, one adapter, three layouts.
2. **Data shape** — `Workout`.
3. **Storage** — `WorkoutDatabaseHelper`.

No repository, no view models, no dependency injection, no coroutines: none of it is
needed for a task of this size.

---
---

# Part 2 — Imports, Called Methods, Java Usage, And The Decisions Behind Them

This part answers, for every line that is not obvious:
**what is imported, what is called, which Java feature is used, and why that option was
chosen instead of the alternatives.**

Totals: **39 imports** across five classes. `Workout.java` has **zero** imports.

---

## 1. Imports — every one, and why it is needed

Imports are only needed for types that are *named in your code*. Anything inherited
(`getString`, `finish`, `setContentView`, `startActivity`) needs no import, and anything
in the same package (`Workout`, `WorkoutAdapter`, `WorkoutDatabaseHelper`) needs no
import either. That is why five classes share only 39 imports.

### 1.1 `MainActivity.java` — 10 imports

| Import | Type it gives you | Why it is here |
|---|---|---|
| `android.content.Intent` | `Intent` | Wraps "go to this screen": `new Intent(this, LogWorkoutActivity.class)` |
| `android.os.Bundle` | `Bundle` | The `onCreate(Bundle savedInstanceState)` parameter — the type must be named in the signature |
| `android.view.View` | `View` | Only for the constants `View.VISIBLE` and `View.GONE` |
| `android.widget.TextView` | `TextView` | Field type for `totalCaloriesText`, `totalWorkoutsText`, `emptyText` |
| `androidx.appcompat.app.AlertDialog` | `AlertDialog` + `AlertDialog.Builder` | The "delete workout?" confirmation dialog |
| `androidx.appcompat.app.AppCompatActivity` | `AppCompatActivity` | The superclass. Gives lifecycle callbacks, `findViewById`, `getString`, Material theme support on old Android versions |
| `androidx.recyclerview.widget.LinearLayoutManager` | `LinearLayoutManager` | Tells the RecyclerView how to arrange rows: vertical, top to bottom |
| `androidx.recyclerview.widget.RecyclerView` | `RecyclerView` | The list container looked up from the layout |
| `com.google.android.material.button.MaterialButton` | `MaterialButton` | Declares `logWorkoutButton` accurately. It is a `View` subclass, so `View` would compile too; naming the real type keeps the code honest about what the layout supplies |
| `java.util.List` | `List<Workout>` | The return type of `getAllWorkouts()` and the loop source |

**Deliberately absent:** no `Context` import (the activity *is* a Context), no
`Toast`/`Snackbar` (this screen never shows one), no `Log` (no debug logging in shipped
code), no `RecyclerView.Adapter` (only the adapter class itself imports that).

### 1.2 `LogWorkoutActivity.java` - 13 imports

| Import | Type it gives you | Why it is here |
|---|---|---|
| `android.os.Bundle` | `Bundle` | `onCreate` parameter type |
| `android.widget.ArrayAdapter` | `ArrayAdapter` | Feeds the Spinner; `ArrayAdapter.createFromResource(...)` is the one-call way to turn a string-array into spinner choices |
| `android.widget.EditText` | `EditText` | Field type for the three inputs (`TextInputEditText` from the XML *is* an `EditText`) |
| `android.widget.Spinner` | `Spinner` | Field type for `intensitySpinner` |
| `android.widget.Toast` | `Toast` | The three short messages: bad numbers, save failed, "Workout saved" |
| `android.view.MenuItem` | `MenuItem` | The app-bar up arrow arrives as a `MenuItem` with id `android.R.id.home`; the id must be compared in `onOptionsItemSelected` |
| `androidx.annotation.NonNull` | `@NonNull` | `onOptionsItemSelected`'s parameter is annotated by the framework; repeating it keeps the override honest |
| `androidx.appcompat.app.AppCompatActivity` | `AppCompatActivity` | Superclass |
| `com.google.android.material.button.MaterialButton` | `MaterialButton` | `saveButton` type |
| `com.google.android.material.textfield.TextInputLayout` | `TextInputLayout` | The outlined wrapper around each field. It is the view that owns the error slot, so validation messages are set on it (`setError`) — the inner `EditText` cannot display an error inside a `TextInputLayout` |
| `java.text.SimpleDateFormat` | `SimpleDateFormat` | Turns "now" into the stored `yyyy-MM-dd` string |
| `java.util.Date` | `Date` | The "now" value handed to the formatter |
| `java.util.Locale` | `Locale` | `Locale.getDefault()` — formatting follows the phone's locale rules instead of the machine default |

**Note:** `getString(...)`, `finish()`, `setContentView(...)` and the fact that `this`
can be passed where a `Context` is required all come from `AppCompatActivity` →
`Context`. That inheritance is why those types are not imported.

### 1.3 `WorkoutAdapter.java` — 9 imports

| Import | Type it gives you | Why it is here |
|---|---|---|
| `android.view.LayoutInflater` | `LayoutInflater` | Builds a row `View` from `item_workout.xml` |
| `android.view.View` | `View` | The inflated row, and `RecyclerView.ViewHolder`'s constructor parameter type |
| `android.view.ViewGroup` | `ViewGroup` | `onCreateViewHolder(ViewGroup parent, int viewType)` — the parent the row is inflated against |
| `android.widget.Button` | `Button` | The row's Delete button (a plain `Button` with `Widget.Material3.Button.TextButton` style, not `MaterialButton`) |
| `android.widget.TextView` | `TextView` | The three text views inside a row |
| `androidx.annotation.NonNull` | `@NonNull` | RecyclerView's contract: the returned holder and the incoming parameters are never null. Also documents intent to the marker |
| `androidx.recyclerview.widget.RecyclerView` | `RecyclerView`, `RecyclerView.Adapter`, `RecyclerView.ViewHolder` | Superclass and the two inner classes that make up the adapter contract |
| `java.util.ArrayList` | `ArrayList` | The backing store that is actually instantiated: `new ArrayList<>()` |
| `java.util.List` | `List<Workout>` | The parameter type of `setWorkouts(...)` |

### 1.4 `Workout.java` — 0 imports

Every field is `long`, `int` or `String` — `java.lang.*` is imported automatically, and
`String` lives there.

### 1.5 `WorkoutDatabaseHelper.java` — 7 imports

| Import | Type it gives you | Why it is here |
|---|---|---|
| `android.content.ContentValues` | `ContentValues` | A column-name → value map. This is the *safe* way to insert: no string concatenation of user input, so no SQL injection |
| `android.content.Context` | `Context` | Constructor parameter, forwarded to `SQLiteOpenHelper`'s constructor (it needs a context to know where the database file lives) |
| `android.database.Cursor` | `Cursor` | The result set returned by `db.query(...)` |
| `android.database.sqlite.SQLiteDatabase` | `SQLiteDatabase` | The handle used for `insert`, `query`, `delete`, `execSQL`; also the parameter type of `onCreate`/`onUpgrade` |
| `android.database.sqlite.SQLiteOpenHelper` | `SQLiteOpenHelper` | Superclass: it handles creating/opening/upgrading the file for you |
| `java.util.ArrayList` | `ArrayList` | The list that is filled row by row |
| `java.util.List` | `List<Workout>` | Return type of `getAllWorkouts()` — a caller should not care that it happens to be an `ArrayList` |

---

## 2. Methods called — file by file

### 2.1 `MainActivity`

| Call | Declared in | What it does here |
|---|---|---|
| `super.onCreate(savedInstanceState)` | `AppCompatActivity` | Runs the framework setup. Skipping it crashes the screen |
| `setContentView(R.layout.activity_main)` | `Activity` | Inflates the XML and makes it the screen's content |
| `new WorkoutDatabaseHelper(this)` | your class | Opens (or creates) `FitnessTrackerDB` lazily |
| `findViewById(int)` | `Activity` | Turns `R.id.xyz` into the real view object |
| `new WorkoutAdapter(this)` | your class | `this` is legal because `MainActivity implements OnDeleteClickListener` |
| `new LinearLayoutManager(this)` | AndroidX | Vertical, top-to-bottom, single-column layout |
| `recyclerView.setLayoutManager(...)` | `RecyclerView` | Must be set before an adapter, otherwise the list cannot be laid out |
| `recyclerView.setAdapter(adapter)` | `RecyclerView` | Connects the data renderer |
| `button.setOnClickListener(lambda)` | `View` | One-line click handling |
| `startActivity(new Intent(this, LogWorkoutActivity.class))` | `Context` / `Activity` | Opens the second screen. The system also calls `onPause` on this screen |
| `super.onResume()` | `AppCompatActivity` | Framework bookkeeping first, then your refresh — the order matters |
| `databaseHelper.getAllWorkouts()` | your class | One read per screen entry |
| `adapter.setWorkouts(list)` | your class | Hands the fresh data to the list |
| `workout.getCaloriesBurned()` | `Workout` | Feeds the running total |
| `getString(int)` / `getString(int, Object...)` | `Context` | Resolves `R.string.xxx`; the two-argument form fills the `%1$d` placeholders |
| `TextView.setText(CharSequence)` | `TextView` | Writes the two summary numbers |
| `List.size()` / `List.isEmpty()` | `java.util.List` | Count for the summary; empty check for the empty-state message |
| `View.setVisibility(int)` | `View` | `View.VISIBLE` / `View.GONE` — hides the empty message when rows exist |
| `new AlertDialog.Builder(this)` | AndroidX | Starts the confirmation dialog |
| `.setTitle(int)` / `.setMessage(CharSequence)` | `AlertDialog.Builder` | Takes resource ids / a formatted message |
| `.setPositiveButton(int, listener)` | `AlertDialog.Builder` | The "Delete" action |
| `.setNegativeButton(int, null)` | `AlertDialog.Builder` | A `null` listener means "just dismiss" — no extra code |
| `.show()` | `AlertDialog.Builder` | Actually displays it |
| `databaseHelper.deleteWorkout(id)` | your class | Removes the row |
| `loadWorkouts()` | your own method | Re-reads after the delete so the UI can never drift from the database |

### 2.2 `LogWorkoutActivity`

| Call | Declared in | What it does here |
|---|---|---|
| `super.onCreate`, `setContentView`, `findViewById` | framework | Same roles as above |
| `ArrayAdapter.createFromResource(this, R.array.intensity_levels, android.R.layout.simple_spinner_item)` | `ArrayAdapter` (static) | Builds the Low/Medium/High adapter directly from a string-array |
| `adapter.setDropDownViewResource(...)` | `ArrayAdapter` | Uses the standard dropdown row for the open list |
| `spinner.setAdapter(...)` | `Spinner` | Connects the choices |
| `EditText.getText()` | `TextView` | Returns an `Editable` |
| `.toString()` | `Object` | `Editable` → `String` |
| `.trim()` | `String` | Removes accidental spaces so `"  "` counts as empty |
| `.isEmpty()` | `String` | The empty-field check (cheaper and clearer than `length() == 0`) |
| `TextInputLayout.setError(CharSequence)` | `TextInputLayout` | Shows the red message under the outlined field. This — not `EditText.setError` — is the call that actually renders inside a `TextInputLayout`; `setError(null)` clears it at the start of each save attempt |
| `Integer.parseInt(String)` | `java.lang.Integer` (static) | Converts the validated strings to numbers |
| `Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()` | `Toast` (static) | Non-blocking feedback; also used for the failure path |
| `new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())` | `java.text` | Formatter for the stored date |
| `.format(new Date())` | `SimpleDateFormat` | Today's date as text |
| `spinner.getSelectedItem().toString()` | `Spinner` / `Object` | The chosen intensity |
| `new Workout(0, type, duration, calories, intensity, date)` | your class | `0` for the id because SQLite's `AUTOINCREMENT` assigns the real one |
| `new WorkoutDatabaseHelper(this)` + `.close()` | your class / `SQLiteOpenHelper` | Opened only for the insert, then closed with `finish()` right behind it |
| `databaseHelper.addWorkout(workout)` | your class | Returns the new row id, or `-1` |
| `finish()` | `Activity` | Closes this screen; `MainActivity.onResume()` then refreshes automatically |
| `onOptionsItemSelected(MenuItem)` | `AppCompatActivity` (overridden) | Receives app-bar clicks. The up arrow sends `android.R.id.home` |
| `MenuItem.getItemId()` | `MenuItem` | Compares the clicked item with `android.R.id.home`; on a match, `finish()` goes back to the previous screen |

### 2.3 `WorkoutAdapter`

| Call | Declared in | What it does here |
|---|---|---|
| `list.clear()` / `list.addAll(newWorkouts)` | `java.util.List` | Keeps one list instance for the adapter's life; no reassignment surprises |
| `notifyDataSetChanged()` | `RecyclerView.Adapter` | Redraws every row — acceptable at this list size |
| `LayoutInflater.from(context).inflate(R.layout.item_workout, parent, false)` | `LayoutInflater` | `false` means "do not attach yet" — RecyclerView attaches the row itself |
| `new WorkoutViewHolder(row)` | your class | Wraps the row's views |
| `holder.itemView` | `RecyclerView.ViewHolder` | The row root, used to reach `getContext()` |
| `context.getString(int, Object...)` | `Context` | Formats `%1$d min - %2$d kcal - %3$s` |
| `setText(CharSequence)` ×3 | `TextView` | Type, details, date |
| `deleteButton.setOnClickListener(lambda)` | `View` | Reports which `Workout` was tapped |
| `deleteClickListener.onDeleteClick(workout)` | your interface | Hands control back to `MainActivity` |
| `super(itemView)`, `findViewById` ×4 | `RecyclerView.ViewHolder` / `View` | Cache row views once, in the constructor |

### 2.4 `WorkoutDatabaseHelper`

| Call | Declared in | What it does here |
|---|---|---|
| `super(context, DATABASE_NAME, null, DATABASE_VERSION)` | `SQLiteOpenHelper` | Registers the file name and schema version; `null` = default cursor factory |
| `db.execSQL(sql)` | `SQLiteDatabase` | Runs DDL: `CREATE TABLE` in `onCreate`, `DROP TABLE IF EXISTS` in `onUpgrade` |
| `getWritableDatabase()` / `getReadableDatabase()` | `SQLiteOpenHelper` | Opens the file if needed; the readable variant is what `query` uses |
| `new ContentValues()` + `.put(key, value)` | `ContentValues` | Builds the row safely. `int` values are autoboxed to `Integer` |
| `db.insert(table, nullColumnHack, values)` | `SQLiteDatabase` | Returns the new row id, or `-1` on failure. `null` for the hack is correct when the map is never empty |
| `db.query(table, null, null, null, null, null, orderBy)` | `SQLiteDatabase` | `null` projection = all six columns; `null` selection = all rows; `"id DESC"` = newest first |
| `cursor.getColumnIndexOrThrow(name)` | `Cursor` | Index looked up once per column instead of per row |
| `cursor.moveToNext()`, `getLong`, `getInt`, `getString` | `Cursor` | Walk the result set |
| `cursor.close()` in `finally` | `Cursor` | Releases the result set even if a read threw |
| `db.delete(table, "id = ?", new String[]{...})` | `SQLiteDatabase` | The `?` placeholder is bound, not concatenated — no injection |
| `String.valueOf(long)` | `java.lang.String` (static) | The bound id as text, since the argument array is `String[]` |
| `new ArrayList<>()` / `.add(...)` | `java.util` | Accumulates the rows |
| `onCreate(db)` from `onUpgrade` | your own class | Reuses the one `CREATE TABLE` statement instead of duplicating it |

---

## 3. Java language features actually used

Nothing here is decorative — each one removes code somewhere else.

| Feature | Where | Why it was the simplest option |
|---|---|---|
| `extends` | both activities (`AppCompatActivity`), helper (`SQLiteOpenHelper`), adapter (`RecyclerView.Adapter<…>`) | The frameworks give lifecycle, DB management and view recycling for free |
| `implements` | `MainActivity implements WorkoutAdapter.OnDeleteClickListener` | A callback without creating an anonymous class or passing a `Context` around |
| Nested `interface` | `OnDeleteClickListener` | One-method contract that a lambda can satisfy |
| Lambda expression | `view -> startActivity(...)`, `view -> saveWorkout()`, `view -> deleteClickListener.onDeleteClick(workout)`, `(dialog, which) -> {...}` | Java 8+ and desugared by AGP; replaces 6–10 lines of anonymous class each |
| Generics `List<Workout>`, `Adapter<ViewHolder>`, `ArrayAdapter<CharSequence>` | everywhere the collection is declared | Compile-time type safety; no casts |
| Diamond operator `new ArrayList<>()` | adapter + helper | No need to repeat the type |
| `static final` constants | table and column names | One place to change a column name; the `static` means no per-instance copy |
| `final` fields | all six `Workout` fields, adapter's `workouts` and listener | Immutability where it is free: a row read from SQLite should not change under the UI |
| Constructor | `Workout(...)`, `WorkoutAdapter(...)`, `WorkoutViewHolder(...)` | No setters, no builders, no factory classes |
| Static nested class | `WorkoutViewHolder` | `static` because it never touches adapter fields; holds the four row views |
| `super.method(...)` | `onCreate`, `onResume`, `ViewHolder` constructor | Required/expected by the framework contracts |
| `@Override` | every lifecycle callback and adapter method | Compiler catches a mistyped signature |
| `@NonNull` | adapter parameters and return values | Documents the RecyclerView contract |
| `try` / `catch` | insert, query, delete, number parsing | Requirement: "proper exception handling in database operations" |
| `finally` | `getAllWorkouts()` | Guarantees `cursor.close()` |
| `for`-each loop | summing calories | `for (Workout workout : workouts)` beats index bookkeeping |
| Ternary operator | `workouts.isEmpty() ? View.VISIBLE : View.GONE` | Replaces a 5-line `if/else` |
| Guard clauses (`if (...) return;`) | `saveWorkout()`, `getAllWorkouts()` inner logic | Flat method, single error at a time, no nesting |
| `String` concatenation | the `CREATE TABLE` statement only | The content is compile-time constants; user data never goes this way (that uses `ContentValues`/`?`) |
| `String.trim()`, `isEmpty()`, `valueOf()` | form validation, delete id | Standard `java.lang.String` methods |
| `Integer.parseInt` + `NumberFormatException` | form validation | The canonical way to detect non-numeric input without regex |
| Autoboxing | `ContentValues.put(String, int)` | The API wants `Object`; the compiler boxes it |
| `long` return for the row id | `addWorkout` | SQLite row ids are 64-bit; `-1` is the documented failure value |

**Deliberately NOT used** (each would add a concept without removing a line):

- no `try`-with-resources (the `finally` block is one line and works on every API level)
- no streams / `Optional` / `var` / records (Java 17 is available, but none of them simplify six fields and a loop)
- no enums for intensity (the Spinner values are `String`s from `strings.xml`, which is what gets stored in the DB anyway)
- no `RecyclerView.Adapter` + `DiffUtil`/`ListAdapter` (a full refresh is one line)
- no `instanceof`, no reflection, no annotations beyond `@Override` and `@NonNull`
- no `Log.*` calls (nothing to debug in shipped code)

---

## 4. How the decisions were made

The rules applied, in order, to every choice below:

1. **Satisfy a grading bullet.** If the task says "Spinner", it is a `Spinner` — a more
   modern dropdown would technically work and would lose marks.
2. **Fewest concepts wins.** When two options both work, the one that needs fewer
   classes, listeners, layers or Gradle configuration is chosen.
3. **Prefer the platform over a library.** Framework types (`SQLiteOpenHelper`,
   `AlertDialog`, `Toast`, `SimpleDateFormat`) before extra dependencies.
4. **One job per class.** `Workout` carries data, the helper owns SQL, the adapter renders
   rows, an activity handles one screen.
5. **Derived data is not stored.** Totals are computed from the list that was already
   read, not kept in a table or a field.

### Decision log — what was chosen and what was rejected

| Decision | Chosen | Rejected because |
|---|---|---|
| Language | Java | The task describes XML layouts + `SQLiteOpenHelper`; Java needs no build plugin change and is what the class notes use |
| UI toolkit | Plain XML layouts + `findViewById` | ViewBinding adds a Gradle flag and generated classes; Compose would not match "activities and layouts" |
| Screen structure | Two activities | The task names "Main Activity" and "Log Workout Activity"; fragments would add a host activity for nothing |
| Data layer | One `WorkoutDatabaseHelper` | Room needs annotations + a compiler plugin + DAO interfaces; the task explicitly asks for `SQLiteOpenHelper` |
| Layer above the helper | None | A repository would only forward three calls |
| State | Re-read on `onResume()` | ViewModel/LiveData exist to survive configuration changes; here the database *is* the source of truth, so a rotation just re-reads |
| Threading | SQLite calls on the UI thread | Tens of rows: the read is sub-millisecond. Executors/AsyncTask would add callbacks for no visible gain — the honest trade-off for a marked practical, not a production app |
| List refresh | `notifyDataSetChanged()` | `DiffUtil` needs a second class and a comparison contract to save a redraw nobody can see at this size |
| Delete confirmation | `androidx.appcompat.app.AlertDialog` | `MaterialAlertDialogBuilder` gives the same Material 3 look for this theme and one more class to explain; both use the theme, so the shipped dialog is already Material-styled |
| Feedback messages | `Toast` | `Snackbar` needs a `CoordinatorLayout` (or a view to anchor to) just to show one line; the form screen has no coordinator |
| Intensity picker | `Spinner` | Required by the task. A `MaterialAutoCompleteTextView` would be nicer and cost marks for ignoring the spec |
| Text fields | `TextInputLayout` + `TextInputEditText`, referenced as `EditText` | The Material wrapper gives the outlined box for the design marks; typing the field as `EditText` keeps the validation code on plain, familiar API |
| Calories total | Java `for` loop | A second `SELECT SUM(...)` would be a second query for data already in memory |
| Workout count | `List.size()` | Same reason — no `COUNT(*)` query |
| Date storage | `TEXT` in `yyyy-MM-dd` | SQLite has no date type; this format sorts correctly as text, which is why the column is declared `TEXT` |
| Date generation | `SimpleDateFormat` | `java.time` would need desugaring config on older `minSdk`; one formatter, one format |
| Validation order | empty → numeric → greater than zero | Cheapest and most specific first; each failure returns immediately so only one message shows at a time |
| Validation display | `TextInputLayout.setError(...)` + one `Toast` for non-numeric | Inline errors under each field are what Material outlined fields are designed for. Setting the error on the wrapper (not the inner `EditText`) is the only form that actually renders; a dialog or a summary `TextView` would be extra UI |
| Status bar / content placement | `android:windowOptOutEdgeToEdgeEnforcement` in the theme | `targetSdk 35` forces edge-to-edge, which made the app bar cover the first field of the form. The alternatives were `enableEdgeToEdge()` + window-inset listeners in both activities (more code), or lowering `targetSdk` (worse practice). One theme attribute keeps the layout correct with no Java changes; the caveat is noted in `themes.xml` for API 36 |
| Back navigation | Manifest `parentActivityName` + `onOptionsItemSelected` handling `android.R.id.home` | The manifest alone only *draws* the up arrow; without the handler the arrow is dead. `NavUtils.navigateUpFromSameTask` or a `Toolbar.setNavigationOnClickListener` would add a class/layout for the same one-line `finish()`, and passing extras back would need `startActivityForResult`, which nothing on the screen sends |
| Column lookup | `getColumnIndexOrThrow` | `getColumnIndex` silently returns `-1`; the `OrThrow` variant fails loudly inside the existing `try/catch` |
| Query projection | `null` (all columns) | Six columns, one table — listing them adds a place to forget to update |
| Insert | `ContentValues` + `db.insert` | Raw `INSERT` with concatenation invites SQL injection and quoting bugs |
| Delete | `db.delete(..., "id = ?", new String[]{...})` | A bound placeholder, not string concatenation |
| Delete + refresh | Delete, then `loadWorkouts()` | One refresh path for every mutation; removing the row from the adapter would be a second, divergent code path |
| Empty state | One `TextView` toggled by `setVisibility` | Better than an empty list with no message; no extra layout file |
| Strings | `strings.xml` with `%1$d` placeholders | Hard-coded text in Java breaks previews, translations and the "readability" marks |
| Icon | One vector drawable | A full mipmap PNG set would be 8+ generated files for the same picture |
| `minSdk` | 26 | Material 3 targets modern devices; 26 keeps Java 17 support simple and covers the vast majority of phones. The task sets no minimum |
| Dependencies | appcompat, material, recyclerview | Nothing else is imported anywhere, so nothing else is declared |

### What would change on a larger app (and why it is not here)

If this screen had to survive rotation with unsaved typed text, or load thousands of
rows, the next steps would be adding a `ViewModel` (state survives rotation), moving the
queries off the main thread, and switching the adapter to `ListAdapter` + `DiffUtil`.
Each of those is a real improvement **for that problem** — and each is an extra concept
that this practical task does not test. They are written here so the reasoning is
explicit: the simplicity is a decision, not an oversight.
