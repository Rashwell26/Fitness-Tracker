package com.example.fitnesstracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a list of Workout objects into rows of item_workout.xml.
 * One class, one job: draw rows and report clicks back to MainActivity.
 */
public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    /** MainActivity implements this so it can confirm and delete the row. */
    public interface OnDeleteClickListener {
        void onDeleteClick(Workout workout);
    }

    private final List<Workout> workouts = new ArrayList<>();
    private final OnDeleteClickListener deleteClickListener;

    public WorkoutAdapter(OnDeleteClickListener deleteClickListener) {
        this.deleteClickListener = deleteClickListener;
    }

    /** Replaces the whole list and redraws the RecyclerView. */
    public void setWorkouts(List<Workout> newWorkouts) {
        workouts.clear();
        workouts.addAll(newWorkouts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workouts.get(position);
        holder.typeText.setText(workout.getWorkoutType());
        holder.detailsText.setText(holder.itemView.getContext().getString(
                R.string.workout_details,
                workout.getDuration(),
                workout.getCaloriesBurned(),
                workout.getIntensity()));
        holder.dateText.setText(workout.getDate());
        holder.deleteButton.setOnClickListener(
                view -> deleteClickListener.onDeleteClick(workout));
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    /** Holds the views of one row so they are looked up only once. */
    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        final TextView typeText;
        final TextView detailsText;
        final TextView dateText;
        final Button deleteButton;

        WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.typeText);
            detailsText = itemView.findViewById(R.id.detailsText);
            dateText = itemView.findViewById(R.id.dateText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
