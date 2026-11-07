package com.example.fitnesstracker;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates and manages the FitnessTrackerDB SQLite database.
 *
 * The only table is Workouts, which stores one row per logged workout.
 * Screens never write SQL themselves, they call the three methods below.
 */
public class WorkoutDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "FitnessTrackerDB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_WORKOUTS = "Workouts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TYPE = "workoutType";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_CALORIES = "caloriesBurned";
    private static final String COLUMN_INTENSITY = "intensity";
    private static final String COLUMN_DATE = "date";

    public WorkoutDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /** Runs once, the first time the database is created. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_WORKOUTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TYPE + " TEXT, "
                + COLUMN_DURATION + " INTEGER, "
                + COLUMN_CALORIES + " INTEGER, "
                + COLUMN_INTENSITY + " TEXT, "
                + COLUMN_DATE + " TEXT)";
        db.execSQL(createTable);
    }

    /**
     * Inserts one workout.
     *
     * @return the new row id, or -1 if the insert failed.
     */
    public long addWorkout(Workout workout) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, workout.getWorkoutType());
        values.put(COLUMN_DURATION, workout.getDuration());
        values.put(COLUMN_CALORIES, workout.getCaloriesBurned());
        values.put(COLUMN_INTENSITY, workout.getIntensity());
        values.put(COLUMN_DATE, workout.getDate());

        try {
            return db.insert(TABLE_WORKOUTS, null, values);
        } catch (Exception e) {

            return -1;
        }
    }

    /**
     * Reads every workout, newest first.
     *
     * @return the list found in the database (empty if there are none or if
     *         the read failed).
     */
    public List<Workout> getAllWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_WORKOUTS, null, null, null, null, null,
                    COLUMN_ID + " DESC");

            int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int typeIndex = cursor.getColumnIndexOrThrow(COLUMN_TYPE);
            int durationIndex = cursor.getColumnIndexOrThrow(COLUMN_DURATION);
            int caloriesIndex = cursor.getColumnIndexOrThrow(COLUMN_CALORIES);
            int intensityIndex = cursor.getColumnIndexOrThrow(COLUMN_INTENSITY);
            int dateIndex = cursor.getColumnIndexOrThrow(COLUMN_DATE);

            while (cursor.moveToNext()) {
                workouts.add(new Workout(
                        cursor.getLong(idIndex),
                        cursor.getString(typeIndex),
                        cursor.getInt(durationIndex),
                        cursor.getInt(caloriesIndex),
                        cursor.getString(intensityIndex),
                        cursor.getString(dateIndex)));
            }
        } catch (Exception e) {
            // Return whatever was read before the error instead of crashing.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return workouts;
    }

    /**
     * Deletes one workout by its id.
     *
     * @return true if a row was deleted.
     */
    public boolean deleteWorkout(long id) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            int rowsDeleted = db.delete(TABLE_WORKOUTS, COLUMN_ID + " = ?",
                    new String[] { String.valueOf(id) });
            return rowsDeleted > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
