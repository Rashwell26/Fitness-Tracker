package com.example.fitnesstracker;

/**
 * One workout record. This is a data class that mirrors
 * a single row in the Workouts table.
 */
public class Workout {

    private final long id;
    private final String workoutType;
    private final int duration;
    private final int caloriesBurned;
    private final String intensity;
    private final String date;

    public Workout(long id, String workoutType, int duration, int caloriesBurned,
            String intensity, String date) {
        this.id = id;
        this.workoutType = workoutType;
        this.duration = duration;
        this.caloriesBurned = caloriesBurned;
        this.intensity = intensity;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public String getWorkoutType() {
        return workoutType;
    }

    public int getDuration() {
        return duration;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public String getIntensity() {
        return intensity;
    }

    public String getDate() {
        return date;
    }
}
