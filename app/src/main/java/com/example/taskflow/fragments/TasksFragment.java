package com.example.taskflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.adapters.TaskAdapter;
import com.example.taskflow.data.Project;
import com.example.taskflow.data.Task;
import com.example.taskflow.data.TaskGrouper;
import com.example.taskflow.data.TaskQuery;
import com.example.taskflow.ui.TaskDialogHelper;
import com.example.taskflow.ui.TaskTouchCallback;
import com.example.taskflow.util.DateHelper;
import com.example.taskflow.util.SimpleTextWatcher;

import java.util.List;

/**
 * The task list. ONE RecyclerView and ONE adapter show every group (Overdue, In Progress,
 * To Do, Done) with section headers. Search + Status / Priority / Date filters + Sort are
 * applied first (through one TaskQuery), then the matching tasks are grouped.
 *
 * Gestures: swipe left = Done, swipe right = delete (after confirmation),
 * long-press and drag = reorder inside the same group.
 */
public class TasksFragment extends ProjectTabFragment
        implements TaskAdapter.Listener, TaskTouchCallback.Listener {

    private interface OnChoice {
        void onChoice(int index);
    }

    private final TaskQuery query = new TaskQuery();
    private TaskAdapter adapter;

    private EditText etSearch;
    private TextView tvStatus;
    private TextView tvPriority;
    private TextView tvDate;
    private TextView tvSort;
    private TextView tvShowing;
    private RecyclerView rvTasks;
    private View emptyNoTasks;
    private View emptyNoMatch;

    public static TasksFragment newInstance(String projectId) {
        return withProjectId(new TasksFragment(), projectId);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            query.restoreFrom(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        query.saveTo(outState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etTaskSearch);
        tvStatus = view.findViewById(R.id.tvFilterStatus);
        tvPriority = view.findViewById(R.id.tvFilterPriority);
        tvDate = view.findViewById(R.id.tvFilterDate);
        tvSort = view.findViewById(R.id.tvFilterSort);
        tvShowing = view.findViewById(R.id.tvShowingTasks);
        rvTasks = view.findViewById(R.id.rvTasks);
        emptyNoTasks = view.findViewById(R.id.emptyNoTasks);
        emptyNoMatch = view.findViewById(R.id.emptyNoMatch);

        // One RecyclerView, one adapter, two view types (header + task).
        adapter = new TaskAdapter(requireContext(), this);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);
        new ItemTouchHelper(new TaskTouchCallback(requireContext(), adapter, this))
                .attachToRecyclerView(rvTasks);

        // Put the saved search text in first, then start listening for changes.
        etSearch.setText(query.search);
        etSearch.addTextChangedListener(new SimpleTextWatcher(text -> {
            query.search = text;
            refreshContent();
        }));

        tvStatus.setOnClickListener(v -> showMenu(v, TaskQuery.STATUS_OPTIONS, query.status + 1, index -> {
            query.status = index - 1;
            refreshContent();
        }));
        tvPriority.setOnClickListener(v -> showMenu(v, TaskQuery.PRIORITY_OPTIONS, query.priority + 1, index -> {
            query.priority = index - 1;
            refreshContent();
        }));
        tvDate.setOnClickListener(v -> showMenu(v, TaskQuery.DATE_OPTIONS, query.date, index -> {
            query.date = index;
            refreshContent();
        }));
        tvSort.setOnClickListener(v -> showMenu(v, TaskQuery.SORT_OPTIONS, query.sort, index -> {
            query.sort = index;
            refreshContent();
        }));

        view.findViewById(R.id.tvResetFilters).setOnClickListener(v -> resetFilters());
        view.findViewById(R.id.btnEmptyResetFilters).setOnClickListener(v -> resetFilters());
        view.findViewById(R.id.btnTasksAddTask).setOnClickListener(
                v -> TaskDialogHelper.showAddTask(requireContext(), host));
        view.findViewById(R.id.btnEmptyAddTask).setOnClickListener(
                v -> TaskDialogHelper.showAddTask(requireContext(), host));

        onRefresh();
    }

    /** Called by the Overview cards: show only this status (or the Overdue date filter). */
    public void applyExternalFilter(int status, int dateFilter) {
        query.search = "";
        query.status = status;
        query.priority = TaskQuery.ANY;
        query.date = dateFilter;
        if (etSearch != null) {
            etSearch.setText("");
        }
        refreshContent();
    }

    private void resetFilters() {
        query.reset();
        if (etSearch != null) {
            etSearch.setText("");
        }
        refreshContent();
    }

    private void showMenu(View anchor, String[] options, int currentIndex, final OnChoice callback) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        for (int i = 0; i < options.length; i++) {
            String label = i == currentIndex ? options[i] + "  \u2713" : options[i];
            popup.getMenu().add(Menu.NONE, i, i, label);
        }
        popup.setOnMenuItemClickListener(item -> {
            callback.onChoice(item.getItemId());
            return true;
        });
        popup.show();
    }

    @Override
    protected void onRefresh() {
        Project project = currentProject();
        int today = DateHelper.today();
        int total = project.getTotalTaskCount();

        // 1) search + filters + sort, 2) group, 3) hand the rows to the adapter (DiffUtil).
        List<Task> visible = query.apply(project.getTasks(), today);
        adapter.submitRows(TaskGrouper.buildRows(visible, today));

        tvShowing.setText("Showing " + visible.size() + " of " + total + " tasks");
        tvStatus.setText("Status: " + TaskQuery.STATUS_SHORT[query.status + 1] + " \u25BE");
        tvPriority.setText("Priority: " + TaskQuery.PRIORITY_SHORT[query.priority + 1] + " \u25BE");
        tvDate.setText("Date: " + TaskQuery.DATE_SHORT[query.date] + " \u25BE");
        tvSort.setText("Sort: " + TaskQuery.SORT_SHORT[query.sort] + " \u25BE");

        // Two different empty states: no tasks at all, versus tasks that don't match.
        if (total == 0) {
            rvTasks.setVisibility(View.GONE);
            emptyNoTasks.setVisibility(View.VISIBLE);
            emptyNoMatch.setVisibility(View.GONE);
        } else if (visible.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            emptyNoTasks.setVisibility(View.GONE);
            emptyNoMatch.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            emptyNoTasks.setVisibility(View.GONE);
            emptyNoMatch.setVisibility(View.GONE);
        }
    }

    // ---------------- TaskAdapter.Listener (taps on a row) ----------------

    /** Ticking the CheckBox sets the status to Done; unticking returns it to To do. */
    @Override
    public void onTaskChecked(String taskId, boolean checked) {
        Task task = currentProject().findTask(taskId);
        if (task != null) {
            task.setStatus(checked ? Task.STATUS_DONE : Task.STATUS_TODO);
        }
        host.onProjectDataChanged();
    }

    @Override
    public void onTaskClicked(String taskId) {
        TaskDialogHelper.showEditTask(requireContext(), host, taskId);
    }

    // ---------------- TaskTouchCallback.Listener (gestures) ----------------

    /** Swipe LEFT: mark the task Done. A task that is already Done is left alone. */
    @Override
    public void onSwipeLeft(String taskId) {
        Task task = currentProject().findTask(taskId);
        if (task == null) {
            restoreRow(taskId);
            return;
        }
        if (task.isDone()) {
            Toast.makeText(requireContext(), "Task already completed", Toast.LENGTH_SHORT).show();
            restoreRow(taskId);
            return;
        }
        task.setStatus(Task.STATUS_DONE);
        host.onProjectDataChanged(); // the row moves to the Done group, header counts update
    }

    /** Swipe RIGHT: ask first. Delete removes the task; Cancel (or closing) puts the row back. */
    @Override
    public void onSwipeRight(final String taskId) {
        Task task = currentProject().findTask(taskId);
        if (task == null) {
            restoreRow(taskId);
            return;
        }
        final boolean[] confirmed = {false};
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete task?")
                .setMessage("\"" + shorten(task.getDescription(), 80) + "\" will be removed from this project.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    confirmed[0] = true;
                    currentProject().removeTask(taskId);
                    host.onProjectDataChanged();
                })
                .setNegativeButton("Cancel", null)
                .setOnDismissListener(dialog -> {
                    if (!confirmed[0]) {
                        restoreRow(taskId);
                    }
                })
                .show();
    }

    /**
     * A drag finished. The on-screen order is saved into the project's own task order and the
     * Sort switches to "Manual" so the list keeps exactly this order.
     */
    @Override
    public void onOrderChanged(List<String> taskIdsInDisplayOrder) {
        currentProject().applyTaskOrder(taskIdsInDisplayOrder);
        query.sort = TaskQuery.SORT_MANUAL;
        host.onProjectDataChanged();
    }

    /** Puts a swiped row back (rebinding it also resets its sideways offset). */
    private void restoreRow(String taskId) {
        int position = adapter.positionOfTask(taskId);
        if (position >= 0) {
            adapter.notifyItemChanged(position);
        }
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "\u2026";
    }
}
