package com.example.ctrl_alt_elite;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskMenuClickListener menuClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance("tasks");

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

        // Visual feedback for completed tasks
        if (task.isCompleted()) {
            holder.titleText.setPaintFlags(holder.titleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.titleText.setAlpha(0.6f);
        } else {
            holder.titleText.setPaintFlags(holder.titleText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.titleText.setAlpha(1.0f);
        }

        // Handle whole task completion
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());
        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            
            // If checking off the whole task, optionally mark all checklist items as done
            List<Map<String, Object>> checklist = task.getChecklist();
            if (isChecked && checklist != null) {
                for (Map<String, Object> item : checklist) {
                    item.put("completed", true);
                }
            }
            
            updateTaskInFirestore(task);
            notifyItemChanged(position);
        });

        // Handle checklist visibility and population
        List<Map<String, Object>> checklist = task.getChecklist();
        if (checklist != null && !checklist.isEmpty()) {
            holder.checklistContainer.setVisibility(View.VISIBLE);
            holder.checklistItemsList.removeAllViews();
            
            for (Map<String, Object> item : checklist) {
                View itemView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_checklist_view, holder.checklistItemsList, false);
                
                CheckBox itemCheckbox = itemView.findViewById(R.id.checklistItemCheckbox);
                TextView itemText = itemView.findViewById(R.id.checklistItemText);
                
                String text = (String) item.get("text");
                itemText.setText(text);
                boolean isItemCompleted = item.containsKey("completed") && (boolean) item.get("completed");
                
                itemCheckbox.setOnCheckedChangeListener(null);
                itemCheckbox.setChecked(isItemCompleted);

                // Strike through individual items
                if (isItemCompleted) {
                    itemText.setPaintFlags(itemText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    itemText.setAlpha(0.6f);
                } else {
                    itemText.setPaintFlags(itemText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    itemText.setAlpha(1.0f);
                }
                
                itemCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.put("completed", isChecked);
                    
                    // If all items are checked, maybe we should check the main task?
                    // For now, just update Firestore
                    updateTaskInFirestore(task);
                    notifyItemChanged(position);
                });
                
                holder.checklistItemsList.addView(itemView);
            }

            holder.markAllDoneButton.setOnClickListener(v -> {
                for (Map<String, Object> item : checklist) {
                    item.put("completed", true);
                }
                updateTaskInFirestore(task);
                notifyItemChanged(position);
            });
        } else {
            holder.checklistContainer.setVisibility(View.GONE);
        }

        holder.menuButton.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onMenuClick(v, task);
            }
        });
    }

    private void updateTaskInFirestore(Task task) {
        db.collection("tasks").document(task.getId()).set(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descText, dateText;
        ImageButton menuButton;
        CheckBox taskCheckbox;
        LinearLayout checklistContainer, checklistItemsList;
        Button markAllDoneButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.taskTitle);
            descText = itemView.findViewById(R.id.taskDescription);
            dateText = itemView.findViewById(R.id.taskDueDate);
            menuButton = itemView.findViewById(R.id.taskMenuButton);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            checklistContainer = itemView.findViewById(R.id.checklistContainer);
            checklistItemsList = itemView.findViewById(R.id.checklistItemsList);
            markAllDoneButton = itemView.findViewById(R.id.markAllDoneButton);
        }
    }
}