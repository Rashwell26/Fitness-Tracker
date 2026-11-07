package com.example.fitnesstracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Landing screen: shows the progress overview, a button to log a workout,
 * and the list of workouts read from SQLite.
 */
public class MainActivity extends AppCompatActivity
        implements WorkoutAdapter.OnDeleteClickListener {

    private WorkoutDatabaseHelper databaseHelper;
    private WorkoutAdapter adapter;
    private TextView totalCaloriesText;
    private TextView totalWorkoutsText;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new WorkoutDatabaseHelper(this);
        totalCaloriesText = findViewById(R.id.totalCaloriesText);
        totalWorkoutsText = findViewById(R.id.totalWorkoutsText);
        emptyText = findViewById(R.id.emptyText);

        // RecyclerView + adapter: the list itself is filled in loadWorkouts().
        adapter = new WorkoutAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.workoutsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        MaterialButton logWorkoutButton = findViewById(R.id.logWorkoutButton);
        logWorkoutButton.setOnClickListener(view ->
                startActivity(new Intent(this, LogWorkoutActivity.class)));
    }

    // onResume runs every time we come back from LogWorkoutActivity,
    // so the list and totals are always up to date.
    @Override
    protected void onResume() {
        super.onResume();
        loadWorkouts();
    }

    /** Reads all workouts and refreshes the summary, list and empty message. */
    private void loadWorkouts() {
        List<Workout> workouts = databaseHelper.getAllWorkouts();
        adapter.setWorkouts(workouts);

        int totalCalories = 0;
        for (Workout workout : workouts) {
            totalCalories += workout.getCaloriesBurned();
        }

        totalCaloriesText.setText(getString(R.string.total_calories_value, totalCalories));
        totalWorkoutsText.setText(getString(R.string.total_workouts_value, workouts.size()));
        emptyText.setVisibility(workouts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Called by the adapter when a row's Delete button is tapped. */
    @Override
    public void onDeleteClick(Workout workout) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, workout.getWorkoutType()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    databaseHelper.deleteWorkout(workout.getId());
                    loadWorkouts();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
