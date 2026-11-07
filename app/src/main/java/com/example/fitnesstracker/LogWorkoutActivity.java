package com.example.fitnesstracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Second screen: the user types in a workout, taps Save, and the row is
 * inserted into SQLite. The screen then closes and MainActivity refreshes.
 */
public class LogWorkoutActivity extends AppCompatActivity {

    private EditText typeInput;
    private EditText durationInput;
    private EditText caloriesInput;
    private Spinner intensitySpinner;

    // The Material wrapper around each field. Errors are shown on the wrapper,
    // because EditText.setError() draws nothing inside a TextInputLayout.
    private TextInputLayout typeLayout;
    private TextInputLayout durationLayout;
    private TextInputLayout caloriesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_workout);

        typeInput = findViewById(R.id.typeInput);
        durationInput = findViewById(R.id.durationInput);
        caloriesInput = findViewById(R.id.caloriesInput);
        intensitySpinner = findViewById(R.id.intensitySpinner);

        typeLayout = findViewById(R.id.typeLayout);
        durationLayout = findViewById(R.id.durationLayout);
        caloriesLayout = findViewById(R.id.caloriesLayout);

        // Fill the Spinner with Low / Medium / High from strings.xml.
        ArrayAdapter<CharSequence> intensityAdapter = ArrayAdapter.createFromResource(
                this, R.array.intensity_levels, android.R.layout.simple_spinner_item);
        intensityAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        intensitySpinner.setAdapter(intensityAdapter);

        MaterialButton saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> saveWorkout());
    }

    /**
     * The system Back button already closes this screen by default; this adds
     * the on-screen arrow so the user has a visible way back.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Validates the form, inserts the workout, and closes the screen. */
    private void saveWorkout() {
        String type = typeInput.getText().toString().trim();
        String durationText = durationInput.getText().toString().trim();
        String caloriesText = caloriesInput.getText().toString().trim();

        // Clear yesterday's message before checking again.
        typeLayout.setError(null);
        durationLayout.setError(null);
        caloriesLayout.setError(null);

        if (type.isEmpty()) {
            typeLayout.setError(getString(R.string.error_type_required));
            return;
        }
        if (durationText.isEmpty()) {
            durationLayout.setError(getString(R.string.error_duration_required));
            return;
        }
        if (caloriesText.isEmpty()) {
            caloriesLayout.setError(getString(R.string.error_calories_required));
            return;
        }

        int duration;
        int calories;
        try {
            duration = Integer.parseInt(durationText);
            calories = Integer.parseInt(caloriesText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_numbers, Toast.LENGTH_SHORT).show();
            return;
        }

        if (duration <= 0) {
            durationLayout.setError(getString(R.string.error_duration_positive));
            return;
        }
        if (calories <= 0) {
            caloriesLayout.setError(getString(R.string.error_calories_positive));
            return;
        }

        // today's date, e.g. 2026-10-28
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        String intensity = intensitySpinner.getSelectedItem().toString();

        Workout workout = new Workout(0, type, duration, calories, intensity, date);
        WorkoutDatabaseHelper databaseHelper = new WorkoutDatabaseHelper(this);
        long newRowId = databaseHelper.addWorkout(workout);
        databaseHelper.close();

        if (newRowId == -1) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.workout_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
