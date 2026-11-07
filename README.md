# Fitness Tracker (Mobile Practical Task 2026)

A deliberately simple Android app for the Fitness Tracking practical task.

- **Java + plain Android views** (no Kotlin, no Compose, no ViewModel, no Room)
- **Two activities:** `MainActivity` (summary + list) and `LogWorkoutActivity` (form)
- **SQLite** through one `SQLiteOpenHelper` class named `WorkoutDatabaseHelper`
- **Database:** `FitnessTrackerDB`, table `Workouts`
- **Material 3** theme and components (`MaterialCardView`, `MaterialButton`, `TextInputLayout`)

## Documentation

| Question | Document |
|---|---|
| What is every file in this project, and what does it do? | [docs/PROJECT_FILES.md](docs/PROJECT_FILES.md) |
| How do I open and run this in Android Studio? | [docs/RUN_IN_ANDROID_STUDIO.md](docs/RUN_IN_ANDROID_STUDIO.md) |
| How do I defend this in the oral assessment? | [docs/DEFENCE_GUIDE.md](docs/DEFENCE_GUIDE.md) |
| What is XML, and how does it build these screens? | [docs/XML_EXPLAINED.md](docs/XML_EXPLAINED.md) |
| How does SQLite work, and what does this app call? | [docs/SQLITE_BRIEF.md](docs/SQLITE_BRIEF.md) |

## How the app works (one paragraph)

`MainActivity` opens, asks `WorkoutDatabaseHelper` for all workouts, and draws them in a
`RecyclerView`. It also adds up the calories and shows the two summary numbers. Tapping
**Log Workout** opens `LogWorkoutActivity`, which validates the form and inserts one row
into SQLite. When that screen closes, `MainActivity.onResume()` runs again, so the list
and totals always show fresh data. Each row has a **Delete** button that asks for
confirmation first.

## Project layout

```
FitnessTracker/
├── build.gradle                  root build file (plugin version)
├── settings.gradle               module list + repositories
├── gradle.properties             Gradle/AndroidX switches
├── local.properties              path to your Android SDK
├── gradlew / gradlew.bat         Gradle wrapper scripts
├── gradle/wrapper/               Gradle wrapper jar + version
├── docs/                         the two documents above
└── app/
    ├── build.gradle              app module config + dependencies
    ├── proguard-rules.pro        empty (release build uses defaults)
    └── src/main/
        ├── AndroidManifest.xml   declares the two activities
        ├── java/com/example/fitnesstracker/
        │   ├── MainActivity.java
        │   ├── LogWorkoutActivity.java
        │   ├── WorkoutAdapter.java
        │   ├── Workout.java
        │   └── WorkoutDatabaseHelper.java
        └── res/
            ├── layout/           activity_main, activity_log_workout, item_workout
            ├── values/           strings, colors, themes
            └── drawable/         ic_launcher
```
