package com.example.taskflow.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.data.Task;
import com.example.taskflow.data.TaskGrouper;
import com.example.taskflow.data.TaskRow;

import java.util.ArrayList;
import java.util.List;

/**
 * The ONE adapter behind the whole grouped Tasks list. getItemViewType() returns
 * TYPE_HEADER for a section header row and TYPE_TASK for a task row, and the matching
 * layout/ViewHolder is created for each.
 *
 * Data updates go through submitRows(): DiffUtil compares the old and new rows and
 * only the changed rows are animated. notifyDataSetChanged() is never used.
 * Rows are identified by task id (or group), never by position.
 */
public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onTaskChecked(String taskId, boolean checked);

        void onTaskClicked(String taskId);
    }

    private static final int[] GROUP_COLORS = {
            R.color.group_overdue, R.color.group_progress, R.color.group_todo, R.color.group_done};

    private final Context context;
    private final Listener listener;
    private final ArrayList<TaskRow> rows = new ArrayList<>();

    public TaskAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Replaces the rows. DiffUtil finds what changed and animates only that. */
    public void submitRows(List<TaskRow> newRows) {
        final ArrayList<TaskRow> oldRows = new ArrayList<>(rows);
        final ArrayList<TaskRow> updated = new ArrayList<>(newRows);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldRows.size();
            }

            @Override
            public int getNewListSize() {
                return updated.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return oldRows.get(oldPosition).isSameItem(updated.get(newPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return oldRows.get(oldPosition).isSameContent(updated.get(newPosition));
            }
        });
        rows.clear();
        rows.addAll(updated);
        result.dispatchUpdatesTo(this);
    }

    // ---------------- queries used by the touch callback and the fragment ----------------

    public boolean isTaskRow(int position) {
        return position >= 0 && position < rows.size() && rows.get(position).isTask();
    }

    public TaskRow getRow(int position) {
        return rows.get(position);
    }

    /** Position of a task's row right now, or -1. Found by task id. */
    public int positionOfTask(String taskId) {
        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            if (row.isTask() && row.taskId.equals(taskId)) {
                return i;
            }
        }
        return -1;
    }

    /** The task ids from top to bottom as they are on screen (headers skipped). */
    public ArrayList<String> taskIdsInDisplayOrder() {
        ArrayList<String> ids = new ArrayList<>();
        for (TaskRow row : rows) {
            if (row.isTask()) {
                ids.add(row.taskId);
            }
        }
        return ids;
    }

    // ---------------- drag and drop ----------------

    /** A row may only move onto another TASK row of the SAME group (never a header, never another group). */
    public boolean canMove(int from, int to) {
        if (from == to || !isTaskRow(from) || !isTaskRow(to)) {
            return false;
        }
        return rows.get(from).group == rows.get(to).group;
    }

    /** Moves one row while it is being dragged. */
    public void moveRow(int from, int to) {
        TaskRow moved = rows.remove(from);
        rows.add(to, moved);
        notifyItemMoved(from, to);
    }

    // ---------------- RecyclerView.Adapter ----------------

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TaskRow.TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.row_task_header, parent, false));
        }
        return new TaskViewHolder(inflater.inflate(R.layout.row_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskRow row = rows.get(position);
        if (row.isHeader()) {
            bindHeader((HeaderViewHolder) holder, row);
        } else {
            bindTask((TaskViewHolder) holder, row);
        }
    }

    private void bindHeader(HeaderViewHolder holder, TaskRow row) {
        int color = context.getColor(GROUP_COLORS[row.group]);
        String title = TaskGrouper.GROUP_TITLES[row.group];
        holder.tvTitle.setText(title);
        holder.tvTitle.setTextColor(color);
        holder.tvCount.setText(String.valueOf(row.count));
        holder.tvCount.setTextColor(color);
        holder.itemView.setContentDescription(title + ", " + row.count + (row.count == 1 ? " task" : " tasks"));
    }

    private void bindTask(TaskViewHolder holder, TaskRow row) {
        final String taskId = row.taskId;
        boolean done = row.status == Task.STATUS_DONE;

        holder.tvDescription.setText(row.description);
        if (done) {
            // Done rows are struck through but keep a dark, readable colour.
            holder.tvDescription.setPaintFlags(holder.tvDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvDescription.setTextColor(context.getColor(R.color.done_text));
        } else {
            holder.tvDescription.setPaintFlags(holder.tvDescription.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvDescription.setTextColor(context.getColor(R.color.text_primary));
        }
        holder.tvMeta.setText(Task.STATUS_LABELS[row.status] + " \u2022 "
                + Task.PRIORITY_LABELS[row.priority] + " \u2022 " + row.dueLabel);
        holder.tvOverdue.setVisibility(row.overdue ? View.VISIBLE : View.GONE);

        // Rows are reused: remove the old click handler before changing the checked state.
        holder.checkBox.setOnClickListener(null);
        holder.checkBox.setChecked(done);
        holder.checkBox.setContentDescription("Mark done: " + row.description);
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onTaskChecked(taskId, ((CheckBox) v).isChecked());
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onTaskClicked(taskId);
            }
        });
    }

    // ---------------- view holders ----------------

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvCount;

        HeaderViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvGroupTitle);
            tvCount = view.findViewById(R.id.tvGroupCount);
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        /** The visible card (the swipe background is drawn behind it). */
        public final View card;
        final CheckBox checkBox;
        final TextView tvDescription;
        final TextView tvMeta;
        final TextView tvOverdue;

        TaskViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.cardTask);
            checkBox = view.findViewById(R.id.cbTaskDone);
            tvDescription = view.findViewById(R.id.tvTaskDescription);
            tvMeta = view.findViewById(R.id.tvTaskMeta);
            tvOverdue = view.findViewById(R.id.tvTaskOverdue);
        }
    }
}
