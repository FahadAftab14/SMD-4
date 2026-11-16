package com.example.taskflow.ui;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.adapters.TaskAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Touch rules for the grouped Tasks list.
 *  - Header rows: no swipe, no drag (their movement flags are 0).
 *  - Task rows: swipe LEFT = mark Done, swipe RIGHT = delete (the fragment asks first),
 *    long-press and drag = reorder, but ONLY inside the same status group.
 */
public class TaskTouchCallback extends ItemTouchHelper.Callback {

    public interface Listener {
        /** Swiped left: the task should be marked Done. */
        void onSwipeLeft(String taskId);

        /** Swiped right: the task should be deleted (after confirmation). */
        void onSwipeRight(String taskId);

        /** A drag finished and the order changed. Ids are in on-screen order, headers skipped. */
        void onOrderChanged(List<String> taskIdsInDisplayOrder);
    }

    private final TaskAdapter adapter;
    private final Listener listener;
    private final SwipeBackgroundPainter painter;
    private final int doneColor;
    private final int deleteColor;
    private boolean moved = false;

    public TaskTouchCallback(Context context, TaskAdapter adapter, Listener listener) {
        this.adapter = adapter;
        this.listener = listener;
        this.painter = new SwipeBackgroundPainter(context);
        this.doneColor = context.getColor(R.color.dot_done);
        this.deleteColor = context.getColor(R.color.overdue_red);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || !adapter.isTaskRow(position)) {
            return makeMovementFlags(0, 0); // headers can neither be dragged nor swiped
        }
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    /** Never allow dropping onto a header or onto a row of another group. */
    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current,
                               @NonNull RecyclerView.ViewHolder target) {
        return adapter.canMove(current.getBindingAdapterPosition(), target.getBindingAdapterPosition());
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        int from = viewHolder.getBindingAdapterPosition();
        int to = target.getBindingAdapterPosition();
        if (!adapter.canMove(from, to)) {
            return false;
        }
        adapter.moveRow(from, to);
        moved = true;
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || !adapter.isTaskRow(position)) {
            return;
        }
        String taskId = adapter.getRow(position).taskId;
        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeLeft(taskId);
        } else {
            listener.onSwipeRight(taskId);
        }
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            moved = false;
            viewHolder.itemView.setAlpha(0.85f); // lifted look while dragging
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(1f);
        if (moved) {
            moved = false;
            final ArrayList<String> ids = adapter.taskIdsInDisplayOrder();
            // Posted so we never change data while RecyclerView is still animating the drop.
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    listener.onOrderChanged(ids);
                }
            });
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE
                && viewHolder instanceof TaskAdapter.TaskViewHolder && dX != 0f) {
            TaskAdapter.TaskViewHolder holder = (TaskAdapter.TaskViewHolder) viewHolder;
            if (dX < 0f) {
                painter.draw(canvas, holder.itemView, holder.card, doneColor, "Done", false);
            } else {
                painter.draw(canvas, holder.itemView, holder.card, deleteColor, "Delete", true);
            }
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
