package com.example.taskflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.data.Project;

/**
 * Home screen list. It extends ListAdapter, so the screen is updated with
 * submitList(newList) and DiffUtil works out which rows really changed.
 * notifyDataSetChanged() is never used.
 *
 * Rows are matched by the project's unique id, never by name or position.
 */
public class ProjectAdapter extends ListAdapter<Project, ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    private static final DiffUtil.ItemCallback<Project> DIFF_CALLBACK = new DiffUtil.ItemCallback<Project>() {
        @Override
        public boolean areItemsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
            return sameItem(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
            return sameContent(oldItem, newItem);
        }
    };

    private final OnProjectClickListener listener;

    public ProjectAdapter(OnProjectClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /** Same project? (same unique id) */
    public static boolean sameItem(Project a, Project b) {
        return a.getId().equals(b.getId());
    }

    /** Is everything a row shows identical? If yes, DiffUtil leaves that row alone. */
    public static boolean sameContent(Project a, Project b) {
        return a.getTag().equals(b.getTag())
                && a.getName().equals(b.getName())
                && a.getDescription().equals(b.getDescription())
                && a.getTotalTaskCount() == b.getTotalTaskCount()
                && a.getDoneTaskCount() == b.getDoneTaskCount()
                && a.getProgressPercent() == b.getProgressPercent()
                && a.getAttachments().size() == b.getAttachments().size()
                && a.getCommentCount() == b.getCommentCount();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_project, parent, false);
        final ProjectViewHolder holder = new ProjectViewHolder(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onProjectClick(getItem(position));
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = getItem(position);
        int total = project.getTotalTaskCount();
        int done = project.getDoneTaskCount();
        int percent = project.getProgressPercent();
        int attachments = project.getAttachments().size();
        int comments = project.getCommentCount();

        holder.tvTag.setText(project.getTag());
        holder.tvName.setText(project.getName());
        holder.tvDescription.setText(project.getDescription());
        holder.tvTasks.setText(done + "/" + total + " tasks");
        holder.tvPercent.setText(percent + "%");
        holder.progress.setMax(100);
        holder.progress.setProgress(percent);
        holder.tvAttachments.setText(attachments + (attachments == 1 ? " Attachment" : " Attachments"));
        holder.tvComments.setText(comments + (comments == 1 ? " Comment" : " Comments"));
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTag;
        final TextView tvName;
        final TextView tvDescription;
        final TextView tvTasks;
        final TextView tvPercent;
        final ProgressBar progress;
        final TextView tvAttachments;
        final TextView tvComments;

        ProjectViewHolder(View row) {
            super(row);
            tvTag = row.findViewById(R.id.tvRowProjectTag);
            tvName = row.findViewById(R.id.tvRowProjectName);
            tvDescription = row.findViewById(R.id.tvRowProjectDescription);
            tvTasks = row.findViewById(R.id.tvRowProjectTasks);
            tvPercent = row.findViewById(R.id.tvRowProjectPercent);
            progress = row.findViewById(R.id.pbRowProjectProgress);
            tvAttachments = row.findViewById(R.id.tvRowProjectAttachments);
            tvComments = row.findViewById(R.id.tvRowProjectComments);
        }
    }
}
