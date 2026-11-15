package com.example.taskflow.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.data.AttachmentCatalog;
import com.example.taskflow.data.AttachmentLink;
import com.example.taskflow.data.CatalogItem;
import com.example.taskflow.util.DateHelper;
import com.example.taskflow.util.ResourceRef;

import java.util.List;

/**
 * Files tab list. Each row: thumbnail (images) or document icon (text), display name,
 * "type - size - linked date", a Preview action and a remove icon.
 * Updated with submitList(); rows are matched by the link's unique id.
 */
public class AttachmentAdapter extends ListAdapter<AttachmentLink, AttachmentAdapter.AttachmentViewHolder> {

    public interface Listener {
        void onPreview(AttachmentLink link);

        void onRemove(AttachmentLink link);
    }

    private static final DiffUtil.ItemCallback<AttachmentLink> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AttachmentLink>() {
                @Override
                public boolean areItemsTheSame(@NonNull AttachmentLink oldItem, @NonNull AttachmentLink newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AttachmentLink oldItem, @NonNull AttachmentLink newItem) {
                    return oldItem.getCatalogId().equals(newItem.getCatalogId())
                            && oldItem.getResourceRef().equals(newItem.getResourceRef())
                            && oldItem.getLinkedAt() == newItem.getLinkedAt();
                }
            };

    private final Context context;
    private final Listener listener;

    public AttachmentAdapter(Context context, Listener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    /** The link shown at a row position (used by the swipe callback). */
    public AttachmentLink getLinkAt(int position) {
        return getItem(position);
    }

    /** Current row position of a link, or -1. Looks the link up by its id. */
    public int positionOfLink(String linkId) {
        List<AttachmentLink> current = getCurrentList();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getId().equals(linkId)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_attachment, parent, false);
        return new AttachmentViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        final AttachmentLink link = getItem(position);
        CatalogItem item = AttachmentCatalog.findById(link.getCatalogId());
        String name = item != null ? item.getDisplayName() : "Unknown file";

        holder.tvName.setText(name);
        if (item != null) {
            holder.tvMeta.setText(item.getTypeLabel() + " \u2022 " + item.getSizeLabel()
                    + " \u2022 Linked " + DateHelper.formatShort(link.getLinkedAt()));
        } else {
            holder.tvMeta.setText("Not in catalog");
        }

        if (item != null && item.isImage()) {
            int resId = ResourceRef.resolve(context, link.getResourceRef());
            if (resId != 0) {
                holder.ivThumb.setImageResource(resId);
            }
            holder.ivThumb.setVisibility(View.VISIBLE);
            holder.tvIcon.setVisibility(View.GONE);
        } else {
            holder.ivThumb.setVisibility(View.GONE);
            holder.tvIcon.setVisibility(View.VISIBLE);
        }

        holder.tvPreview.setContentDescription("Preview " + name);
        holder.tvRemove.setContentDescription("Remove " + name);
        holder.tvPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onPreview(link);
            }
        });
        holder.tvRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRemove(link);
            }
        });
    }

    public static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        /** The visible card (the swipe background is drawn behind it). */
        public final View card;
        final ImageView ivThumb;
        final TextView tvIcon;
        final TextView tvName;
        final TextView tvMeta;
        final TextView tvPreview;
        final TextView tvRemove;

        AttachmentViewHolder(View row) {
            super(row);
            card = row.findViewById(R.id.cardAttachment);
            ivThumb = row.findViewById(R.id.ivAttachmentThumb);
            tvIcon = row.findViewById(R.id.tvAttachmentIcon);
            tvName = row.findViewById(R.id.tvAttachmentName);
            tvMeta = row.findViewById(R.id.tvAttachmentMeta);
            tvPreview = row.findViewById(R.id.tvAttachmentPreview);
            tvRemove = row.findViewById(R.id.tvAttachmentRemove);
        }
    }
}
