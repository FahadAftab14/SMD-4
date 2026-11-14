package com.example.taskflow.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a (searched, filtered, sorted) list of tasks into the rows of the grouped
 * Tasks screen. Fixed group order: Overdue, In progress, To do, Done.
 * Every task lands in exactly ONE group, and a group with no tasks gets no header.
 *
 * Overdue is checked first: an unfinished task whose due date has passed is shown
 * in Overdue (not in To do / In progress). Done tasks are never overdue.
 */
public final class TaskGrouper {

    public static final int GROUP_OVERDUE = 0;
    public static final int GROUP_IN_PROGRESS = 1;
    public static final int GROUP_TODO = 2;
    public static final int GROUP_DONE = 3;
    public static final int GROUP_COUNT = 4;

    public static final String[] GROUP_TITLES = {"Overdue", "In Progress", "To Do", "Done"};

    private TaskGrouper() {
    }

    public static int groupOf(Task task, int today) {
        if (task.isOverdue(today)) {
            return GROUP_OVERDUE;
        }
        switch (task.getStatus()) {
            case Task.STATUS_IN_PROGRESS:
                return GROUP_IN_PROGRESS;
            case Task.STATUS_DONE:
                return GROUP_DONE;
            default:
                return GROUP_TODO;
        }
    }

    /** The tasks keep the order they have in "tasks" inside each group. */
    public static ArrayList<TaskRow> buildRows(List<Task> tasks, int today) {
        ArrayList<ArrayList<Task>> buckets = new ArrayList<>();
        for (int g = 0; g < GROUP_COUNT; g++) {
            buckets.add(new ArrayList<Task>());
        }
        for (Task task : tasks) {
            buckets.get(groupOf(task, today)).add(task);
        }

        ArrayList<TaskRow> rows = new ArrayList<>();
        for (int g = 0; g < GROUP_COUNT; g++) {
            ArrayList<Task> bucket = buckets.get(g);
            if (bucket.isEmpty()) {
                continue; // no header for an empty group
            }
            rows.add(TaskRow.header(g, bucket.size()));
            for (Task task : bucket) {
                rows.add(TaskRow.task(task, g, today));
            }
        }
        return rows;
    }
}
