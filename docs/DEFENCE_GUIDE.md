# Defending This Project - Plain English Guide

Everything you need to answer questions about the app, in the simplest words that still
sound correct. Read section 1 and 2 first. Then read the Q&A in section 10 out loud.

---

## 0. The 20-second answer (memorise this)

> "It is a fitness tracker Android app. The user logs a workout on one screen - type,
> duration, calories, intensity - and it is saved into a SQLite database. The main screen
> reads that database and shows a list of workouts, plus the total workouts and total
> calories burned. You can delete a workout, and it asks for confirmation first."

That is the whole app. Everything else is detail.

---

## 1. What the app does

Two screens.

**Screen 1 - Main Activity (the landing screen)**
- Shows two numbers: workouts logged and total calories burned.
- Shows a list of recent workouts (newest first).
- Has a **Log Workout** button.
- Shows "No workouts logged yet" when the list is empty.

**Screen 2 - Log Workout Activity**
- Three text boxes: workout type, duration in minutes, calories burned.
- One dropdown (Spinner): Low / Medium / High.
- One **Save Workout** button.
- If something is wrong, a red message appears under the box.

**Deleting** happens on screen 1: every row has a Delete button, and a dialog asks
"Delete workout?" before anything is removed.

---

## 2. The big picture - three layers, in simple words

The app is split into three jobs. Nothing is shared between them except the data class.

```
        THE SCREENS                    THE DATA SHAPE          THE STORAGE
  +---------------------+
  | MainActivity        |---- reads ---------+
  |  shows totals+list  |                   |
  +---------------------+                   |
            |                               v
            |                     +-------------------+
            |                     | Workout.java      |   one workout
            |                     | 6 simple fields   |   = one object
            |                     +-------------------+
            v                               ^
  +---------------------+                   |
  | LogWorkoutActivity  |---- saves --------+
  |  form + validation  |
  +---------------------+
            |
            v
  +-------------------------------------------------+
  | WorkoutDatabaseHelper                           |
  |  addWorkout / getAllWorkouts / deleteWorkout      |
  +-------------------------------------------------+
            |
            v
  +-------------------------------------------------+
  | SQLite file: FitnessTrackerDB                   |
  | table: Workouts                                 |
  +-------------------------------------------------+
```

Say it like this:

> "The activities never write SQL themselves. They call three methods on
> WorkoutDatabaseHelper. That keeps the database code in one file, so if a column changes
> I change it in one place."

That single sentence already answers most architecture questions.

---

## 3. Follow one workout through the app (the story to tell)

This is the best thing to rehearse. It proves you understand the whole path.

1. User taps **Log Workout**. MainActivity starts LogWorkoutActivity with an `Intent`.
2. User types the details and taps **Save Workout**.
3. `saveWorkout()` checks the input: empty, then numbers, then greater than zero.
4. It builds a `Workout` object. The id is `0` because SQLite has not given it an id yet.
5. It calls `databaseHelper.addWorkout(workout)`.
6. Inside the helper, `ContentValues` collects the six values, and `db.insert(...)` writes
   one row. SQLite fills in the real id and returns it. `-1` means the insert failed.
7. The screen shows "Workout saved" and calls `finish()`, so it closes.
8. MainActivity's `onResume()` runs automatically. It calls `getAllWorkouts()`.
9. The helper runs `db.query(...)` ordered by `id DESC` and turns each cursor row into a
   `Workout` object.
10. The adapter is given the list, and the RecyclerView builds one card per workout.
11. MainActivity loops through the same list once to add up the calories, then updates the
    two numbers at the top.

That is the entire app in 11 steps. If you can say steps 3 to 11 out loud, you can defend
the code.

---

## 4. The database, explained simply

### What SQLite is

SQLite is a database that lives in **one file** on the phone. There is no server, no
internet, no password. Android has it built in, so there was nothing to install.

### The four facts to memorise

| Thing | Value |
|---|---|
| Database file name | `FitnessTrackerDB` |
| Table name | `Workouts` |
| Number of columns | 6 |
| Database version | 1 |

### The table

| Column | Type | Meaning |
|---|---|---|
| `id` | INTEGER PRIMARY KEY AUTOINCREMENT | unique number, given by SQLite |
| `workoutType` | TEXT | "Running", "Cycling" |
| `duration` | INTEGER | minutes |
| `caloriesBurned` | INTEGER | calories |
| `intensity` | TEXT | Low / Medium / High |
| `date` | TEXT | "2026-11-05" |

The CREATE statement in `onCreate()`:

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

Points you can make about it:

- **`AUTOINCREMENT`** means SQLite numbers the rows itself: 1, 2, 3... So the app never
  has to work out the next id, and two rows can never share one.
- **The column names here match the field names in `Workout.java`.** That is deliberate -
  it makes the mapping obvious and avoids confusion.
- **`date` is TEXT** because SQLite has no date type. `yyyy-MM-dd` is used because that
  format sorts correctly as text - oldest to newest.
- **`onCreate()` runs once**, the first time the database is created. After that the file
  already exists, so Android does not call it again.
- **`onUpgrade()`** drops the table and recreates it. It only runs if the version number is
  increased, which is how you would change the schema later.

### The three methods, in plain words

| Method | What it does | What it returns |
|---|---|---|
| `addWorkout(Workout)` | builds `ContentValues` and runs `db.insert(...)` | the new row id, or `-1` on failure |
| `getAllWorkouts()` | runs `db.query(...)`, walks the cursor, builds objects | a `List<Workout>`, empty if there is nothing |
| `deleteWorkout(long id)` | runs `db.delete(... "id = ?" ...)` | `true` if a row was removed |

### Two security points examiners like

1. **Insert uses `ContentValues`, not string building.** The values are never glued into
   SQL text, so a workout type like `'); DROP TABLE Workouts; --` is stored as plain text
   and cannot damage anything.
2. **Delete uses `?` and a separate argument array.** `"id = ?"` with
   `new String[]{String.valueOf(id)}` is a *bound parameter*. The value cannot become part
   of the command. Same reason as above.

### Exception handling

Every database method is wrapped in `try` / `catch`:

- `addWorkout` returns `-1` instead of crashing.
- `getAllWorkouts` returns whatever it managed to read. The cursor is closed in a
  `finally` block, so it is closed even if an error happens.
- `deleteWorkout` returns `false`.

The screen then decides what to show: the save failure shows a toast, and a failed read
just shows the empty message. **The app never crashes because of the database.** That is
the sentence to use.

---

## 5. The screens - which widget, and why

### Main screen (`activity_main.xml`)

| Widget | Why it is there |
|---|---|
| `MaterialCardView` | holds the two summary numbers, so they look like a panel, not plain text |
| `TextView` (x2) | "Workouts logged" and "Total calories burned" values |
| `MaterialButton` | "Log Workout" - a filled Material button |
| `TextView` | the heading "Recent workouts" |
| `RecyclerView` | the scrolling list of workouts |
| `TextView` (`emptyText`) | the "No workouts logged yet" message, hidden when there are rows |

### Form screen (`activity_log_workout.xml`)

| Widget | Why it is there |
|---|---|
| `ScrollView` | so the form still works on small screens when the keyboard is open |
| `TextInputLayout` (x3) | the outlined box around each field - this is a Material component |
| `TextInputEditText` (x3) | the actual text boxes |
| `TextView` | the label "Intensity" |
| `Spinner` | the Low / Medium / High dropdown |
| `MaterialButton` | "Save Workout" |

### One row of the list (`item_workout.xml`)

`MaterialCardView` containing:

- the workout type, in bold, with a **Delete** text button on the same line
- a details line: `30 min - 250 kcal - High`
- the date

### Why RecyclerView and not ListView

RecyclerView only creates the rows that are on screen, and reuses them when you scroll. A
ListView recreates them. RecyclerView is the modern, recommended widget.

### The three parts of a RecyclerView - say it like this

> "The RecyclerView is the container. The adapter is the bridge between my list and the
> rows - it creates rows and fills them. The ViewHolder holds the four views of one row, so
> `findViewById` runs once per row instead of on every scroll."

| Piece | Job | In my code |
|---|---|---|
| Adapter | says how many rows, creates them, fills them | `WorkoutAdapter` |
| ViewHolder | holds one row's views | `WorkoutAdapter.WorkoutViewHolder` |
| LayoutManager | arranges rows vertically | `LinearLayoutManager` |

Key methods to name: `getItemCount()`, `onCreateViewHolder()`, `onBindViewHolder()`.

### The Spinner

> "The Spinner is filled from a string-array in `strings.xml` using
> `ArrayAdapter.createFromResource`. The selected value is read with
> `getSelectedItem().toString()` and saved as text."

### Why the error appears on `TextInputLayout` and not on the `EditText`

This is a real bug you fixed, and it is a good thing to mention because it shows you tested
your own work:

> "At first I called `setError` on the EditText, and nothing appeared on screen. In a
> Material outlined field the wrapper owns the error slot, so the message has to be set on
> the `TextInputLayout`. I moved the call and the message showed."

### Material Design - the points you made

- Theme `Theme.Material3.DayNight` - Material 3, and dark mode comes for free.
- One green palette in `colors.xml` (`colorPrimary` and friends), used by the whole app.
- Material components: `MaterialCardView`, `MaterialButton`, `TextInputLayout`,
  `Widget.Material3.Button.TextButton` for the row's delete action.
- Spacing is consistent: 16dp screen padding, 12dp between fields, 8dp between rows, 56dp
  tall main buttons.
- The delete button uses `?attr/colorError`, so it reads as a destructive action.
- All text lives in `strings.xml`, so nothing is hard-coded in Java.

---

## 6. How the screen stays up to date (lifecycle)

Two lifecycle methods are used:

| Method | When it runs | What it does here |
|---|---|---|
| `onCreate()` | once, when the screen is first created | finds the views, sets up the RecyclerView, wires the button |
| `onResume()` | every time the screen becomes visible again | calls `loadWorkouts()` to re-read the database |

The line to say:

> "I reload in `onResume` instead of `onCreate` because `onResume` also runs when I come
> back from the form. So a saved workout appears immediately, with no manual refresh."

**What about rotating the phone?** The activity is recreated, `onCreate` and `onResume` run
again, and the data is re-read from SQLite. Nothing is lost. That is why there is no
ViewModel: the database already holds the truth, so there is no state to preserve in
memory.

---

## 7. Validation - the order, and why that order

`saveWorkout()` checks in this order and stops at the first problem:

1. **Is the field empty?** `trim()` first, so spaces only count as empty.
2. **Is it a number?** `Integer.parseInt` inside `try/catch NumberFormatException` -
   catches "abc".
3. **Is it greater than zero?** Duration 0 or calories 0 makes no sense.

Why this order: it is the cheapest and most specific check first, and stopping early means
only one message is on screen at a time, so the user knows exactly what to fix.

The message appears under the offending field, not in a dialog. Toasts are used only for
three short feedback moments: "not a number", "could not save", and "Workout saved".

---

## 8. Three real problems you found and fixed (great defence material)

Mention these - they show you tested rather than assumed.

**1. The app bar covered the first field on the form.**
Cause: `targetSdk 35` turns on edge-to-edge drawing on Android 15 and newer, so the content
starts at the very top and the app bar is painted over it. Fix: one theme attribute,
`android:windowOptOutEdgeToEdgeEnforcement="true"`, which brings back the normal layout -
status bar, then app bar, then content.

**2. Validation looked broken.**
Cause: `EditText.setError(...)` draws nothing inside a `TextInputLayout`. Fix: set the error
on the `TextInputLayout` itself.

**3. The up arrow did nothing.**
Cause: the manifest's `parentActivityName` makes AppCompat *draw* the arrow, but nothing
handled the click. Fix: override `onOptionsItemSelected` and call `finish()` when the id is
`android.R.id.home`.

---

## 9. Numbers to have ready

| Question | Answer |
|---|---|
| How many activities? | 2 - `MainActivity`, `LogWorkoutActivity` |
| How many Java files? | 5 - 2 activities, 1 adapter, 1 model, 1 database helper |
| How many layouts? | 3 - main, form, one row |
| How many tables? | 1 - `Workouts` |
| How many columns? | 6 |
| Database name / version | `FitnessTrackerDB` / 1 |
| Package name | `com.example.fitnesstracker` |
| Minimum Android version | Android 8.0 (API 26) |
| Target Android version | API 35 |
| Language | Java 17 |
| Build tool | Gradle 8.9, Android Gradle Plugin 8.7.3 |
| Libraries | appcompat, material, recyclerview - three only |
| Where is the APK | `app\build\outputs\apk\debug\app-debug.apk` |

---

## 10. Likely questions and short answers

**Q: What is the app for?**
A fitness tracker. You log workouts and it keeps a history and totals.

**Q: Where is the data stored?**
In a SQLite database called `FitnessTrackerDB`, table `Workouts`, in the app's private
storage. It stays there after the app closes.

**Q: Why SQLite and not a web service?**
The data is small and personal, and SQLite works offline with no login and no server. It
also matches the task requirement.

**Q: Why two activities?**
Each screen has one job. The form is a separate screen that the system can open, close and
go back from without extra code.

**Q: How does a new workout get saved?**
`saveWorkout()` validates, builds a `Workout`, calls `addWorkout()`, which puts the values
in `ContentValues` and runs `db.insert(...)`.

**Q: How does the list know about it?**
`onResume()` re-reads the database and hands the list to the adapter, which redraws the
rows.

**Q: How do you delete?**
The row's Delete button reports the `Workout` back to `MainActivity` through an interface.
MainActivity shows a confirmation dialog. Only if the user taps Delete does
`deleteWorkout(id)` run, and then the list reloads.

**Q: How do you protect against SQL injection?**
Insert uses `ContentValues`, and delete uses a bound `?` placeholder. User text never
becomes part of the SQL command.

**Q: What happens if the database fails?**
Every method has `try/catch`. Insert returns `-1` and shows a toast. Read returns an empty
list. Delete returns `false`. The app does not crash.

**Q: Does the database connection get closed?**
Yes, after the insert: `LogWorkoutActivity` calls `close()` before `finish()`.
`MainActivity` keeps one helper for as long as the screen exists, because it re-reads on
every `onResume`, and Android closes it with the app.

**Q: Why is the cursor closed in `finally`?**
So it is closed even if reading a row throws. Leaked cursors waste memory and log warnings.

**Q: What is a ViewHolder for?**
It stores the four views of a row, so the app looks them up once instead of on every
scroll. That keeps scrolling smooth.

**Q: Why `notifyDataSetChanged()`?**
The list is small and the whole thing is replaced after each change, so redrawing
everything is the simplest correct option. With thousands of rows I would use `DiffUtil`.

**Q: Why is the id set to 0 in Java?**
`0` is a placeholder. SQLite's `AUTOINCREMENT` assigns the real id when the row is
inserted, and `insert()` returns it.

**Q: Why store the date as text?**
SQLite has no date type. `yyyy-MM-dd` is sortable as text, which is why the column is
`TEXT`.

**Q: Where does the date come from?**
`SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())` applied to `new Date()`, so it uses
the phone's locale rules.

**Q: What if the user leaves a field empty?**
The red message appears under that field and nothing is saved. Empty is checked before the
number check.

**Q: Can they go back without saving?**
Yes. The up arrow calls `finish()`, and the system Back button does the same by default.
Nothing is written unless Save is tapped.

**Q: Does the database work on the main thread?**
Yes, on purpose. A few rows read in well under a millisecond. For a large dataset I would
move the queries to a background thread or use a ViewModel and coroutines.

**Q: Why not Room?**
Room adds a compiler plugin and DAO interfaces for queries that are three lines long. The
task asked for `SQLiteOpenHelper`, so that is what is used - and it shows the SQL.

**Q: Why Java and not Kotlin?**
The task is about layouts and `SQLiteOpenHelper`. Java needs no extra plugin and matches
the course notes.

**Q: How is it Material Design?**
Material 3 theme, `MaterialCardView`, `MaterialButton`, `TextInputLayout` with outlined
boxes, one green colour palette, consistent spacing, and `colorError` on delete.

**Q: What did you have to fix during testing?**
The app bar covering the form (edge-to-edge on Android 15), validation errors not showing
because `setError` was on the wrong view, and an up arrow that had no click handler.

**Q: What would you improve?**
Edit existing workouts, a chart of calories per week, empty/loading states on a background
thread, and maybe a reminder to log.

---

## 11. Live demo script (about 2 minutes)

1. **Open the app.** Point at the top: "zero workouts, zero calories" and the empty
   message. "That message is a TextView that is hidden as soon as there is data."
2. **Tap Log Workout.** Point at the three fields, then open the Spinner. "The spinner is
   filled from a string-array, Low, Medium and High."
3. **Tap Save with everything empty.** "The field turns red with a message. Empty is
   checked first, then that it is a number, then that it is greater than zero."
4. **Type Running, 30, 250, High and save.** Back on the main screen: one row and the
   numbers changed. "That row came from the database, not from memory."
5. **Add Cycling, 45, 400, Low.** Totals become 2 workouts and 650 kcal.
6. **Tap Delete on a row.** The dialog appears. "It asks first, so you cannot delete by
   accident." Tap Delete. Totals drop.
7. **Close the app fully and reopen it.** "The workouts are still here, because they are in
   SQLite, not in memory."
8. **Optional: open Database Inspector** (View > Tool Windows > App Inspection >
   Database Inspector > `FitnessTrackerDB` > `Workouts`) and show the live rows.

If the examiner asks you to prove something, this demo proves nearly every requirement:
UI, validation, insert, read, totals, delete with confirmation, and persistence.

---

## 12. Honest limitations (say these confidently, not apologetically)

- No editing a workout - only adding and deleting. The task did not ask for edit.
- No user accounts or sync - the data is local to the phone.
- Database work happens on the main thread - fine for a handful of rows.
- No charts - the progress overview is two totals, as the task specifies.
- Deleting is permanent, with no undo.

Then one sentence: "If this were a real product, the next steps would be edit support, a
weekly chart, and moving the queries off the main thread."

---

## 13. Where is that in the code?

| If they ask about... | Open this |
|---|---|
| The screens | `res/layout/activity_main.xml`, `activity_log_workout.xml`, `item_workout.xml` |
| The list behaviour | `WorkoutAdapter.java` |
| Save logic and validation | `LogWorkoutActivity.java`, method `saveWorkout()` |
| Totals and delete dialog | `MainActivity.java`, methods `loadWorkouts()` and `onDeleteClick()` |
| The SQL | `WorkoutDatabaseHelper.java`, `onCreate()` |
| The columns | `WorkoutDatabaseHelper.java` constants, and `Workout.java` fields |
| Colours and theme | `res/values/colors.xml`, `themes.xml` |
| Text and messages | `res/values/strings.xml` |
| Which screen opens first | `AndroidManifest.xml` |
| App name, version, Android versions | `app/build.gradle` |

---

## 14. Vocabulary sheet - one line each

| Word | What it means |
|---|---|
| Activity | One screen with its own layout |
| Intent | A message that opens another screen |
| Layout | The XML file describing what is on screen |
| Inflate | Turn XML into real view objects |
| Widget / View | A single thing on screen: text, button, list |
| Adapter | Bridge between a data list and a RecyclerView |
| ViewHolder | Holds one row's views so they are found only once |
| Cursor | A bookmark that moves down the rows of a query result |
| ContentValues | A key-value map of column names to values for a write |
| SQLiteOpenHelper | Android class that creates and opens the database file |
| Lifecycle | The order Android calls onCreate, onResume, onPause, onDestroy |
| Toast | Small message that appears and fades away |
| Dialog | A small window that needs an answer before continuing |
| Callback / interface | A way for one class to tell another "this happened" |
| Material Design | Google's design system for look, spacing and components |

---

## 15. If you get stuck

Three safe sentences:

1. "The activity reads and writes only through `WorkoutDatabaseHelper`."
2. "The RecyclerView is fed by the adapter, and the adapter gets the list from the
   database."
3. "Every database call is inside a try/catch, so a failure cannot crash the app."

Then say: "I can show you that part in the code" - and open the file from section 13.
