package com.example.taskflow.data;

import com.example.taskflow.util.DateHelper;

/**
 * One row of the grouped task list. A row is either a section HEADER
 * ("Overdue  2") or a TASK.
 *
 * A task row is a SNAPSHOT: it copies the values it needs and does not keep the
 * Task object. This matters for DiffUtil: a Task is edited in place, so if the
 * old and new rows pointed at the same Task they would always look identical.
 * With snapshots, DiffUtil can compare "before" and "after" correctly.
 */
public final class TaskRow {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TASK = 1;

    public final int type;
    /** Which group this row belongs to (TaskGrouper.GROUP_*). */
    public final int group;

    // header rows
    public final int count;

    // task rows
    public final String taskId;
    public final String description;
    public final int status;
    public final int priority;
    public final String dueLabel;
    public final boolean overdue;

    private TaskRow(int type, int group, int count, String taskId, String description,
                    int status, int priority, String dueLabel, boolean overdue) {
        this.type = type;
        this.group = group;
        this.count = count;
        this.taskId = taskId;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueLabel = dueLabel;
        this.overdue = overdue;
    }

    public static TaskRow header(int group, int count) {
        return new TaskRow(TYPE_HEADER, group, count, null, null, 0, 0, null, false);
    }

    public static TaskRow task(Task task, int group, int today) {
        String due = DateHelper.format(task.getDueDate());
        if (task.getDueDate() == today) {
            due = due + " (today)";
        }
        return new TaskRow(TYPE_TASK, group, 0, task.getId(), task.getDescription(),
                task.getStatus(), task.getPriority(), due, task.isOverdue(today));
    }

    public boolean isHeader() {
        return type == TYPE_HEADER;
    }

    public boolean isTask() {
        return type == TYPE_TASK;
    }

    /** DiffUtil: do the two rows stand for the same thing? (same group header, or same task id) */
    public boolean isSameItem(TaskRow other) {
        if (type != other.type) {
            return false;
        }
        return isHeader() ? group == other.group : taskId.equals(other.taskId);
    }

    /** DiffUtil: is everything that is drawn on the row identical? */
    public boolean isSameContent(TaskRow other) {
        if (type != other.type || group != other.group) {
            return false;
        }
        if (isHeader()) {
            return count == other.count;
        }
        return description.equals(other.description)
                && status == other.status
                && priority == other.priority
                && overdue == other.overdue
                && dueLabel.equals(other.dueLabel);
    }
}
