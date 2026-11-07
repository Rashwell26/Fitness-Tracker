# SQLite Briefing - What It Is, How It Works, And How This App Uses It

A working explanation of the database under this project, from "what is a database file" up
to the exact method calls in `WorkoutDatabaseHelper.java`.

---

## 1. The short version

SQLite is a small database engine that lives **inside your app**. There is no server, no
network, no username or password. The whole database is **one file** on the phone. Your app
opens that file with the SQLite library that Android already includes, reads and writes rows,
and closes it.

That is why this app works on a plane, in a lift, or with mobile data off.

---

## 2. What "database" means here

| Word | Meaning |
|---|---|
| Database | one file holding one or more tables |
| Table | rows and columns, like a spreadsheet |
| Row / record | one item - here, one workout |
| Column / field | one attribute - here, type, duration, calories... |
| Query | a request: "give me the rows ordered by id" |
| Cursor | the result of a query, read one row at a time |
| Schema | the table definitions - names, types, keys |

In this project there is exactly one table, `Workouts`, with six columns.

---

## 3. What is special about SQLite (the facts worth knowing)

1. **Embedded, not a server.** The engine is a library linked into the app. Nothing listens
   on a port. There is no daemon to start.
2. **One file.** `FitnessTrackerDB` on Android lives in the app's private folder
   (`/data/data/com.example.fitnesstracker/databases/`). Copy that file and you have the whole
   database.
3. **Public domain.** Free for any use, which is why it is in Android, browsers, phones,
   cars and aircraft.
4. **Transactional and safe.** A write either fully happens or does not happen at all. A
   crash mid-write cannot leave a half-written row.
5. **Typed loosely.** Column types are *hints*, not strict rules (section 5).
6. **Single writer.** Only one write at a time; reads can run alongside when write-ahead
   logging is on. For a phone app with one user that is irrelevant, and it is why SQLite
   feels instant here.
7. **Very small, very big.** The library is a few hundred KB, and the database file can grow
   to enormous sizes in principle (hundreds of terabytes). Our file is about 20 KB.

---

## 4. How it works inside the file

You do not need this to pass, but it explains *why* certain rules exist.

- The file is divided into **pages**, usually 4096 bytes. Every read or write works on pages.
- Each table is stored as a **B-tree**: an index-like structure that keeps rows sorted by
  their `rowid`, so looking one up does not mean scanning every row.
- The **schema** (the `CREATE TABLE` text) is itself stored in a table called
  `sqlite_master`. That is why the database can describe itself.
- A **rollback journal** (or **WAL** file) is written next to the database before changes are
  applied. If the app is killed mid-write, SQLite uses the journal on next open to undo the
  partial change. This is how the "all or nothing" promise is kept.
- Every row has a hidden **rowid** unless the table declares `WITHOUT ROWID`. Our `id
  INTEGER PRIMARY KEY` *is* that rowid, stored under a friendly name.
- `AUTOINCREMENT` makes SQLite keep a small internal table, `sqlite_sequence`, that remembers
  the highest id ever used, so ids are never reused even after deletes. If you inspect this
  database you will see that table - it is normal.

---

## 5. Types: SQLite is deliberately loose

SQLite has five real storage types: `NULL`, `INTEGER`, `REAL`, `TEXT`, `BLOB`.
A column's declared type is a **type affinity** - a preference. If you put text into an
INTEGER column, SQLite will store text rather than refuse.

| Declared in this project | Affinity | What it holds |
|---|---|---|
| `INTEGER` (`id`, `duration`, `caloriesBurned`) | INTEGER | whole numbers |
| `TEXT` (`workoutType`, `intensity`, `date`) | TEXT | strings |

Two practical consequences:

- There is no date type, so `date` is `TEXT`. The format `yyyy-MM-dd` was chosen because it
  sorts correctly as text: `2026-11-05` comes after `2026-10-31` both as a date and as a
  string.
- Validation therefore matters. The database will happily store `duration` as "abc" if the
  app lets it through. Our form checks that the text is a number *before* building the
  object - that is the app's job, not SQLite's.

---

## 6. The SQL this project actually runs

Only four statements exist in the whole app.

**Create (once, at first launch)**

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

- `PRIMARY KEY` means unique and never null.
- `AUTOINCREMENT` means SQLite assigns 1, 2, 3... and never reuses a number.

**Insert (every save)**

The app does not write an INSERT string. It fills `ContentValues` and calls
`db.insert("Workouts", null, values)`, and Android builds the statement safely:

```sql
INSERT INTO Workouts (workoutType, duration, caloriesBurned, intensity, date)
VALUES (?, ?, ?, ?, ?)
```

**Select (every time the main screen appears)**

`db.query("Workouts", null, null, null, null, null, "id DESC")` becomes:

```sql
SELECT * FROM Workouts ORDER BY id DESC
```

`null` projection means all columns; `null` selection means every row; `id DESC` puts the
newest first. No `WHERE` clause is needed because the app always shows everything.

**Delete (after confirmation)**

```sql
DELETE FROM Workouts WHERE id = ?
```

**Drop (only if the schema version is increased)**

```sql
DROP TABLE IF EXISTS Workouts
```

---

## 7. The Android API, function by function

The app never touches the SQLite C library. It uses three Android classes.

### 7.1 `SQLiteOpenHelper` - the file manager

This class opens the database file, creates it the first time, and upgrades it when the
version changes. `WorkoutDatabaseHelper` extends it.

| Method | When it runs | In this project |
|---|---|---|
| constructor `(Context, name, CursorFactory, version)` | when you `new` the helper | called once per screen, with `FitnessTrackerDB` and version `1` |
| `onCreate(SQLiteDatabase)` | **once**, when the file does not exist yet | runs the `CREATE TABLE` |
| `onUpgrade(SQLiteDatabase, int, int)` | when the stored version is lower than the code's | drops and recreates the table |
| `getWritableDatabase()` | first write | opens the file for writing |
| `getReadableDatabase()` | first read | opens it for reading (usually the same handle) |
| `close()` | when finished | called in `LogWorkoutActivity` after the insert |

How the version is stored: SQLite has a single number, `PRAGMA user_version`, inside the
file. The helper compares it with the `DATABASE_VERSION` you pass in. That is the whole
mechanism - no extra table, no config file.

Why this matters: if you add a column later and bump the version to 2, `onUpgrade` runs on
existing installs. Without bumping, existing users keep the old table and the new code
fails with "no such column".

### 7.2 `SQLiteDatabase` - the statement runner

| Method | Signature in this project | Returns |
|---|---|---|
| `insert` | `insert("Workouts", null, values)` | new row id, or `-1` if nothing was inserted |
| `query` | `query("Workouts", null, null, null, null, null, "id DESC")` | a `Cursor` |
| `delete` | `delete("Workouts", "id = ?", new String[]{id})` | number of rows deleted |
| `execSQL` | `execSQL("CREATE TABLE ...")` | nothing |
| `update` | not used - the app has no edit feature | number of rows updated |

`query` has seven parameters and beginners often mix them up. In order:

```
table, columns, selection, selectionArgs, groupBy, having, orderBy
```

Everything except `table` and `orderBy` is `null` here, because the app wants every row,
every column, in id order.

The `null` in `insert(table, null, values)` is the "null column hack": if `values` were
empty, SQLite needs at least one column named to form valid SQL. Our `ContentValues` is never
empty, so `null` is correct.

### 7.3 `ContentValues` - a safety box for values

```java
ContentValues values = new ContentValues();
values.put(WorkoutDatabaseHelper.COLUMN_TYPE, workout.getWorkoutType());
values.put(WorkoutDatabaseHelper.COLUMN_DURATION, workout.getDuration());
```

It is a map of column name to value. `put` accepts `String`, `Integer`, `Long`, `Double`,
`Boolean` - the `int` fields are boxed automatically.

**Why not build the SQL string?** Two reasons:

1. **Injection.** A type like `x'); DROP TABLE Workouts; --` would become part of the
   command. `ContentValues` sends values separately from the SQL, so they can never be read as
   commands.
2. **Quoting.** Apostrophes (`O'Brien's run`), nulls and numbers all have to be escaped
   correctly. The map does that for you.

### 7.4 `Cursor` - reading the result

A `Cursor` is a bookmark that starts before the first row. You move it down one row at a
time and read each column by index.

| Method | Meaning |
|---|---|
| `getCount()` | how many rows came back |
| `moveToFirst()` / `moveToNext()` | move the bookmark; returns false when there are no more rows |
| `getColumnIndexOrThrow("date")` | find the column's position by name |
| `getString(int)` / `getInt(int)` / `getLong(int)` | read the value at that position |
| `close()` | release it |

The code in `getAllWorkouts()`:

```java
cursor = db.query(TABLE_WORKOUTS, null, null, null, null, null, COLUMN_ID + " DESC");
int typeIndex = cursor.getColumnIndexOrThrow(COLUMN_TYPE);
while (cursor.moveToNext()) {
    workouts.add(new Workout(
            cursor.getLong(idIndex),
            cursor.getString(typeIndex), ...));
}
```

Two habits that are worth defending:

- **Column indexes are looked up once, before the loop**, not inside it. Looking them up per
  row would be needless work.
- **`getColumnIndexOrThrow` instead of `getColumnIndex`.** The plain version returns `-1`
  for a misspelled column, and `getString(-1)` then throws a confusing error. The `OrThrow`
  version fails immediately with a clear message, and the whole block is inside `try/catch`.

### 7.5 `close()` - why it is in `finally`

```java
} finally {
    if (cursor != null) {
        cursor.close();
    }
}
```

`finally` runs whether the read succeeded or threw. Cursors hold a window onto the result
set; leaving them open leaks memory and logs `CursorWindow ... finalized without being
closed` warnings. This is the single most common SQLite mistake in student projects, so it
is worth pointing at.

---

## 8. What happens, step by step, on a phone

**First launch, main screen:**

1. `new WorkoutDatabaseHelper(this)` runs. Nothing is created yet - the constructor only
   remembers the name and version.
2. `getAllWorkouts()` calls `getReadableDatabase()`.
3. Android looks for `databases/FitnessTrackerDB`. It is missing, so it creates the file and
   calls `onCreate(db)`, which runs the `CREATE TABLE`.
4. The query returns zero rows, so the list is empty and the empty message shows.

**Every save:**

1. `LogWorkoutActivity` builds a `Workout` with id `0`.
2. `addWorkout` calls `getWritableDatabase()`, fills `ContentValues`, calls `insert`.
3. SQLite writes a page, updates the journal, commits. `insert` returns the new id (1, 2, 3...).
4. The helper is closed, the screen finishes.

**Every delete:**

1. The user confirms in the dialog.
2. `deleteWorkout(id)` runs `delete` with `"id = ?"`.
3. The row is removed; the returned count is turned into `true`/`false`.
4. `loadWorkouts()` re-reads, so the totals drop.

**After a phone restart:** nothing special happens. The file is on disk; the same helper
opens the same file and the same version, so `onCreate` is skipped and the data is simply
there. That is the persistence requirement proven.

---

## 9. Transactions and speed

By default **each statement is its own transaction**: it is committed before the call
returns. That is why one insert is instant.

If you had to write 1,000 rows, doing 1,000 separate inserts means 1,000 commits, which is
slow. The fix is one explicit transaction:

```java
db.beginTransaction();
try {
    // many inserts
    db.setTransactionSuccessful();   // commit
} finally {
    db.endTransaction();             // rollback if not marked successful
}
```

This project does not need it - a workout is saved one at a time, by a human. Being able to
say *why* it is not needed is better than adding it.

**Threading:** our queries run on the main (UI) thread. For a handful of rows the read is
sub-millisecond, so there is no visible delay. On a large table this would cause jank, and
the standard answer is to run the work on a background thread (`Executor`, coroutines) or
through Room/WorkManager. That is an honest limitation, not a hidden bug.

---

## 10. Indexes (and why this app has none)

An index is a sorted side-table that makes lookups fast - like the index at the back of a
book. You add one when you frequently filter or sort on a column of a large table:

```sql
CREATE INDEX idx_workouts_date ON Workouts(date);
```

Here the app always reads **all** rows ordered by `id`, and `id` is the primary key (already
indexed by the B-tree). An extra index would slow writes and use space for no gain. Again:
absence is a decision, not an oversight.

---

## 11. How this project's helper maps to the task requirements

| Requirement | Where it is satisfied |
|---|---|
| Database named `FitnessTrackerDB` | `DATABASE_NAME` constant |
| Table named `Workouts` | `TABLE_WORKOUTS` constant |
| Six columns with the exact names and types | `onCreate()` statement |
| `id INTEGER PRIMARY KEY AUTOINCREMENT` | first line of the CREATE |
| `SQLiteOpenHelper` named `WorkoutDatabaseHelper` | the class itself |
| Add workout | `addWorkout(Workout)` |
| Retrieve all workouts | `getAllWorkouts()` |
| Delete workout | `deleteWorkout(long id)` |
| Exception handling | `try/catch` in all three; cursor closed in `finally` |

Column names are constants (`COLUMN_TYPE` etc.) so a rename happens in one place, and the
`Workout` fields use the same names, so the mapping is obvious at a glance.

---

## 12. Mistakes that bite people (and how this app avoids them)

| Mistake | Symptom | Avoided by |
|---|---|---|
| Building SQL with `+` and user text | SQL injection, crashes on `'` | `ContentValues` and `?` binding |
| Not closing the cursor | memory warnings, slow app | `finally { cursor.close(); }` |
| Assuming a query returns one row | `CursorIndexOutOfBounds` | `while (cursor.moveToNext())` |
| Using `getColumnIndex` then `-1` | cryptic crash | `getColumnIndexOrThrow` |
| Adding a column without bumping the version | `no such column` on old installs | version constant + `onUpgrade` |
| Forgetting to update the table in `onCreate` **and** `onUpgrade` | new installs and upgrades disagree | `onUpgrade` drops and calls `onCreate` |
| Storing a date with `Date.toString()` | cannot sort, cannot compare | fixed `yyyy-MM-dd` text |
| Letting the database validate input | bad rows saved | checks in `saveWorkout()` before insert |
| Doing a huge import on the UI thread | frozen screen | not needed here; documented trade-off |

---

## 13. How to look inside the database yourself

Three ways, best first:

1. **Database Inspector** (Android Studio): View > Tool Windows > App Inspection >
   Database Inspector, pick `com.example.fitnesstracker`, open `FitnessTrackerDB` >
   `Workouts`. Rows update live as you use the app.
2. **Command line on a device/emulator:**
   ```
   adb shell run-as com.example.fitnesstracker ls databases
   adb exec-out run-as com.example.fitnesstracker cat databases/FitnessTrackerDB > ft.db
   ```
   Then open `ft.db` in any SQLite viewer. (Use `exec-out`, not `shell cat` - plain
   `shell` corrupts binary output.)
3. **From code, temporarily:** `Log.d("DB", DatabaseUtils.dumpCursorToString(cursor))`.

Note: the file only appears after the app has opened the database at least once. A freshly
installed app that has never shown the list has no `databases/` folder yet.

---

## 14. Questions you may be asked about SQLite

**Q: Is SQLite a server?**
No. It is a library that reads and writes a file on the device.

**Q: Where is the file?**
`/data/data/com.example.fitnesstracker/databases/FitnessTrackerDB`, private to the app.

**Q: What happens if two rows get the same id?**
They cannot. `id` is the primary key and SQLite generates it.

**Q: What if the user uninstalls the app?**
The database is deleted with the app's data. Reinstalling starts empty.

**Q: What if the app crashes during a save?**
The write either completed or was rolled back when the file is next opened. There is no
half-saved row.

**Q: Why version 1, and what does the version do?**
It is the schema version. `onUpgrade` runs when the number in code is higher than the number
stored in the file.

**Q: Could two people use it at once?**
Only one write at a time, and it is a single-device app with one user, so that never comes up.

**Q: Is the data encrypted?**
No. It is private to the app by Android's file permissions. Encryption would need SQLCipher.

**Q: Why not save to a text file or SharedPreferences?**
Those suit a few settings. A list of records with totals and deletes is relational data -
queries, ordering and atomic writes come free with SQLite.

**Q: How would you add "edit workout"?**
Add `updateWorkout` using `db.update(...)` with `"id = ?"`, open the form with the id in the
`Intent`, and pre-fill the fields. Everything else stays the same.

---

## 15. Cheat sheet

```sql
-- create
CREATE TABLE Workouts (id INTEGER PRIMARY KEY AUTOINCREMENT, workoutType TEXT, ...);

-- insert
INSERT INTO Workouts (workoutType, duration) VALUES (?, ?);

-- read all, newest first
SELECT * FROM Workouts ORDER BY id DESC;

-- read with a filter
SELECT * FROM Workouts WHERE intensity = ?;

-- totals (not used here - the app sums the loaded list instead)
SELECT COUNT(*), SUM(caloriesBurned) FROM Workouts;

-- delete one
DELETE FROM Workouts WHERE id = ?;

-- change one
UPDATE Workouts SET duration = ? WHERE id = ?;

-- inspect the schema
SELECT sql FROM sqlite_master WHERE type = 'table';
```

```java
// Android equivalents used in this project
new WorkoutDatabaseHelper(context);          // helper
getWritableDatabase() / getReadableDatabase();
new ContentValues(); values.put(column, value);
db.insert(table, null, values);              // returns row id or -1
db.query(table, null, null, null, null, null, "id DESC");   // returns Cursor
db.delete(table, "id = ?", new String[]{id});
db.execSQL(createTableStatement);

cursor.getColumnIndexOrThrow(name); getLong(i); getInt(i); getString(i);
cursor.moveToNext(); cursor.close();
```

---

## 16. The one-paragraph answer

> "SQLite is an embedded database: one file, no server. My app opens `FitnessTrackerDB` with
> a `SQLiteOpenHelper` called `WorkoutDatabaseHelper`, which creates the `Workouts` table on
> first use. Saving inserts one row through `ContentValues`, so the values are bound rather
> than concatenated into SQL. Reading runs a query ordered by id and walks the cursor,
> closing it in a finally block. Deleting uses a bound `id = ?`. Every method is wrapped in
> try/catch, so a database problem returns an empty list or -1 instead of crashing the app."
