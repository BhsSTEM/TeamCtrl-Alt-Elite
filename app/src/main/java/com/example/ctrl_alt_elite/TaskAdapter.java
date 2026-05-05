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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskMenuClickListener menuClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance("tasks");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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

        // Show extra details (Assigned To and Tractor)
        StringBuilder details = new StringBuilder();
        if (task.getAssignedTo() != null && !task.getAssignedTo().isEmpty()) {
            details.append("Assigned to: ").append(task.getAssignedTo());
        }
        if (task.getTractorId() != null && !task.getTractorId().isEmpty()) {
            if (details.length() > 0) details.append(" | ");
            details.append("Tractor: ").append(task.getTractorId());
        }
        holder.detailsText.setText(details.toString());
        holder.detailsText.setVisibility(details.length() > 0 ? View.VISIBLE : View.GONE);

        // Show Repeat Interval
        if (task.getRepeatInterval() != null && !task.getRepeatInterval().equalsIgnoreCase("None") && !task.getRepeatInterval().isEmpty()) {
            holder.repeatDisplayText.setText(task.getRepeatInterval());
            holder.repeatDisplayText.setVisibility(View.VISIBLE);
        } else {
            holder.repeatDisplayText.setVisibility(View.GONE);
        }

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
            if (isChecked && task.getRepeatInterval() != null && !task.getRepeatInterval().equalsIgnoreCase("None") && !task.getRepeatInterval().isEmpty()) {
                // Repeat Logic: Update date, uncheck, and reset checklist
                handleRepeatingTask(task, position);
            } else {
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
            }
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

    private void handleRepeatingTask(Task task, int position) {
        String currentDueDateStr = task.getDueDate();
        String interval = task.getRepeatInterval();
        
        try {
            Date currentDate = dateFormat.parse(currentDueDateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentDate);

            if (interval.equalsIgnoreCase("Daily")) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            } else if (interval.equalsIgnoreCase("Weekly")) {
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            } else if (interval.equalsIgnoreCase("Monthly")) {
                cal.add(Calendar.MONTH, 1);
            }

            String newDueDate = dateFormat.format(cal.getTime());
            task.setDueDate(newDueDate);
            task.setCompleted(false); // Uncheck it

            // Reset checklist
            List<Map<String, Object>> checklist = task.getChecklist();
            if (checklist != null) {
                for (Map<String, Object> item : checklist) {
                    item.put("completed", false);
                }
            }

            updateTaskInFirestore(task);
            notifyItemChanged(position);


        } catch (ParseException e) {
            // If date parsing fails, just mark it completed as normal
            task.setCompleted(true);
            updateTaskInFirestore(task);
            notifyItemChanged(position);
        }
    }

    private void updateTaskInFirestore(Task task) {
        db.collection("tasks").document(task.getId()).set(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descText, dateText, detailsText, repeatDisplayText;
        ImageButton menuButton;
        CheckBox taskCheckbox;
        LinearLayout checklistContainer, checklistItemsList;
        Button markAllDoneButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.taskTitle);
            descText = itemView.findViewById(R.id.taskDescription);
            dateText = itemView.findViewById(R.id.taskDueDate);
            detailsText = itemView.findViewById(R.id.taskDetails);
            repeatDisplayText = itemView.findViewById(R.id.taskRepeatDisplay);
            menuButton = itemView.findViewById(R.id.taskMenuButton);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            checklistContainer = itemView.findViewById(R.id.checklistContainer);
            checklistItemsList = itemView.findViewById(R.id.checklistItemsList);
            markAllDoneButton = itemView.findViewById(R.id.markAllDoneButton);
        }
    }
}