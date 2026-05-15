package com.example.ctrl_alt_elite;

import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
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
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private SimpleDateFormat completionDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    private String currentUserName;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int DELAY_MS = 2000; // 2 seconds linger delay

    public interface OnTaskMenuClickListener {
        void onMenuClick(View view, Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskMenuClickListener menuClickListener) {
        this.taskList = taskList;
        this.menuClickListener = menuClickListener;
    }

    public void setCurrentUserName(String userName) {
        this.currentUserName = userName;
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
        updateVisualFeedback(holder, task);

        // Handle whole task completion
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());
        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            if (isChecked) {
                // Mark all checklist items as done locally
                List<Map<String, Object>> checklist = task.getChecklist();
                if (checklist != null) {
                    for (Map<String, Object> item : checklist) {
                        item.put("completed", true);
                    }
                }
            }

            if (isChecked && task.getRepeatInterval() != null && !task.getRepeatInterval().equalsIgnoreCase("None") && !task.getRepeatInterval().isEmpty()) {
                // Repeat Logic: Update date, uncheck, and reset checklist after delay
                task.setCompleted(true);
                updateVisualFeedback(holder, task);
                notifyItemChanged(adapterPos);
                handler.postDelayed(() -> handleRepeatingTask(task, adapterPos), DELAY_MS);
            } else {
                task.setCompleted(isChecked);
                if (isChecked) {
                    String userName = currentUserName != null ? currentUserName : buttonView.getContext().getString(R.string.tasks_unknown_user);
                    task.setCompletedBy(userName);
                    task.setCompletedDate(completionDateFormat.format(new Date()));
                    
                    updateVisualFeedback(holder, task);
                    notifyItemChanged(adapterPos);
                    handler.postDelayed(() -> updateTaskInFirestore(task), DELAY_MS);
                } else {
                    task.setCompletedBy(null);
                    task.setCompletedDate(null);
                    updateTaskInFirestore(task);
                    updateVisualFeedback(holder, task);
                    notifyItemChanged(adapterPos);
                }
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
                    
                    // If any item is unchecked, the main task should also be unchecked
                    if (!isChecked && task.isCompleted()) {
                        task.setCompleted(false);
                        task.setCompletedBy(null);
                        task.setCompletedDate(null);
                    }
                    
                    updateTaskInFirestore(task);
                    notifyItemChanged(holder.getAdapterPosition());
                });
                
                holder.checklistItemsList.addView(itemView);
            }

            holder.markAllDoneButton.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return;

                // Mark all checklist items as done
                for (Map<String, Object> item : checklist) {
                    item.put("completed", true);
                }
                
                // Mark the task itself as complete as well
                if (task.getRepeatInterval() != null && !task.getRepeatInterval().equalsIgnoreCase("None") && !task.getRepeatInterval().isEmpty()) {
                    task.setCompleted(true);
                    updateVisualFeedback(holder, task);
                    notifyItemChanged(adapterPos);
                    handler.postDelayed(() -> handleRepeatingTask(task, adapterPos), DELAY_MS);
                } else {
                    task.setCompleted(true);
                    String userName = currentUserName != null ? currentUserName : v.getContext().getString(R.string.tasks_unknown_user);
                    task.setCompletedBy(userName);
                    task.setCompletedDate(completionDateFormat.format(new Date()));
                    
                    updateVisualFeedback(holder, task);
                    notifyItemChanged(adapterPos);
                    handler.postDelayed(() -> updateTaskInFirestore(task), DELAY_MS);
                }
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

    private void updateVisualFeedback(TaskViewHolder holder, Task task) {
        if (task.isCompleted()) {
            holder.titleText.setPaintFlags(holder.titleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.titleText.setAlpha(0.6f);
            
            if (task.getCompletedBy() != null && task.getCompletedDate() != null) {
                String completionInfo = holder.itemView.getContext().getString(R.string.tasks_completed_by_format, task.getCompletedBy(), task.getCompletedDate());
                holder.completionInfoText.setText(completionInfo);
                holder.completionInfoText.setVisibility(View.VISIBLE);
            } else {
                holder.completionInfoText.setVisibility(View.GONE);
            }
        } else {
            holder.titleText.setPaintFlags(holder.titleText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.titleText.setAlpha(1.0f);
            holder.completionInfoText.setVisibility(View.GONE);
        }
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());
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
            task.setCompletedBy(null);
            task.setCompletedDate(null);

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
            String userName = currentUserName != null ? currentUserName : "Unknown";
            task.setCompletedBy(userName);
            task.setCompletedDate(completionDateFormat.format(new Date()));
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
        TextView titleText, descText, dateText, detailsText, repeatDisplayText, completionInfoText;
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
            completionInfoText = itemView.findViewById(R.id.taskCompletionInfo);
            menuButton = itemView.findViewById(R.id.taskMenuButton);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            checklistContainer = itemView.findViewById(R.id.checklistContainer);
            checklistItemsList = itemView.findViewById(R.id.checklistItemsList);
            markAllDoneButton = itemView.findViewById(R.id.markAllDoneButton);
        }
    }
}
