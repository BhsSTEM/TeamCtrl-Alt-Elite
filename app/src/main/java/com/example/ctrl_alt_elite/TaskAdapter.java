package com.example.ctrl_alt_elite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskMenuClickListener menuClickListener;

    public interface OnTaskMenuClickListener {
        void onMenuClick(View view, Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskMenuClickListener menuClickListener) {
        this.taskList = taskList;
        this.menuClickListener = menuClickListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleText.setText(task.getTitle());
        holder.descText.setText(task.getDescription());
        holder.dateText.setText(task.getDueDate());

        holder.menuButton.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onMenuClick(v, task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descText, dateText;
        ImageButton menuButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.taskTitle);
            descText = itemView.findViewById(R.id.taskDescription);
            dateText = itemView.findViewById(R.id.taskDueDate);
            menuButton = itemView.findViewById(R.id.taskMenuButton);
        }
    }
}