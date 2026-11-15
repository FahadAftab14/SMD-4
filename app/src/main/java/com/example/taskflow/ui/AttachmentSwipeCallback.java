package com.example.taskflow.ui;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.adapters.AttachmentAdapter;
import com.example.taskflow.data.AttachmentLink;

/** Files tab: swiping an attachment row to the RIGHT asks to remove it. No drag, no left swipe. */
public class AttachmentSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface Listener {
        void onSwipedRight(AttachmentLink link);
    }

    private final AttachmentAdapter adapter;
    private final Listener listener;
    private final SwipeBackgroundPainter painter;
    private final int removeColor;

    public AttachmentSwipeCallback(Context context, AttachmentAdapter adapter, Listener listener) {
        super(0, ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.listener = listener;
        this.painter = new SwipeBackgroundPainter(context);
        this.removeColor = context.getColor(R.color.overdue_red);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        listener.onSwipedRight(adapter.getLinkAt(position));
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE
                && viewHolder instanceof AttachmentAdapter.AttachmentViewHolder && dX > 0f) {
            AttachmentAdapter.AttachmentViewHolder holder = (AttachmentAdapter.AttachmentViewHolder) viewHolder;
            painter.draw(canvas, holder.itemView, holder.card, removeColor, "Remove", true);
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
