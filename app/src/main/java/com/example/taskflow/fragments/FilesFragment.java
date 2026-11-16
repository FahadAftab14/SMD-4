package com.example.taskflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.adapters.AttachmentAdapter;
import com.example.taskflow.data.AttachmentCatalog;
import com.example.taskflow.data.AttachmentLink;
import com.example.taskflow.data.CatalogItem;
import com.example.taskflow.data.Project;
import com.example.taskflow.ui.AttachmentPreviewDialog;
import com.example.taskflow.ui.AttachmentSwipeCallback;
import com.example.taskflow.ui.CatalogPickerDialog;
import com.example.taskflow.ui.RemoveAttachmentDialog;
import com.example.taskflow.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.Locale;

/**
 * The project's attachment links in a RecyclerView: search + All/Images/Text chips,
 * Preview, add from the catalog, and remove (by the row's remove icon OR by swiping the
 * row to the right) with typed-name confirmation. Chip counts always refer to the
 * complete collection, not the filtered list.
 */
public class FilesFragment extends ProjectTabFragment
        implements AttachmentAdapter.Listener, AttachmentSwipeCallback.Listener {

    private static final int TYPE_ALL = 0;
    private static final int TYPE_IMAGES = 1;
    private static final int TYPE_TEXT = 2;

    private static final String STATE_SEARCH = "files_search";
    private static final String STATE_TYPE = "files_type";

    private String search = "";
    private int typeFilter = TYPE_ALL;
    private AttachmentAdapter adapter;

    private EditText etSearch;
    private TextView chipAll;
    private TextView chipImages;
    private TextView chipText;
    private TextView tvShowing;
    private RecyclerView rvFiles;
    private View emptyNoAttachments;
    private View emptyNoMatch;

    public static FilesFragment newInstance(String projectId) {
        return withProjectId(new FilesFragment(), projectId);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            search = savedInstanceState.getString(STATE_SEARCH, "");
            typeFilter = savedInstanceState.getInt(STATE_TYPE, TYPE_ALL);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SEARCH, search);
        outState.putInt(STATE_TYPE, typeFilter);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etAttachmentSearch);
        chipAll = view.findViewById(R.id.chipAll);
        chipImages = view.findViewById(R.id.chipImages);
        chipText = view.findViewById(R.id.chipText);
        tvShowing = view.findViewById(R.id.tvShowingAttachments);
        rvFiles = view.findViewById(R.id.rvFiles);
        emptyNoAttachments = view.findViewById(R.id.emptyNoAttachments);
        emptyNoMatch = view.findViewById(R.id.emptyNoAttachmentMatch);

        adapter = new AttachmentAdapter(requireContext(), this);
        rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFiles.setAdapter(adapter);
        new ItemTouchHelper(new AttachmentSwipeCallback(requireContext(), adapter, this))
                .attachToRecyclerView(rvFiles);

        etSearch.setText(search);
        etSearch.addTextChangedListener(new SimpleTextWatcher(text -> {
            search = text;
            refreshContent();
        }));

        chipAll.setOnClickListener(v -> selectType(TYPE_ALL));
        chipImages.setOnClickListener(v -> selectType(TYPE_IMAGES));
        chipText.setOnClickListener(v -> selectType(TYPE_TEXT));

        view.findViewById(R.id.btnFilesAddAttachment).setOnClickListener(v -> showAddAttachmentDialog());
        view.findViewById(R.id.btnEmptyAddAttachment).setOnClickListener(v -> showAddAttachmentDialog());
        view.findViewById(R.id.btnEmptyResetAttachments).setOnClickListener(v -> resetFilters());

        onRefresh();
    }

    private void selectType(int type) {
        typeFilter = type;
        refreshContent();
    }

    private void resetFilters() {
        search = "";
        typeFilter = TYPE_ALL;
        if (etSearch != null) {
            etSearch.setText("");
        }
        refreshContent();
    }

    /** Single-choice catalog dialog: only unlinked items, Add disabled until one is selected. */
    private void showAddAttachmentDialog() {
        CatalogPickerDialog.show(requireContext(), "Add attachment", "Add",
                currentProject().getLinkedCatalogIds(), item -> {
                    currentProject().addAttachment(AttachmentCatalog.createLink(item));
                    host.onProjectDataChanged();
                });
    }

    @Override
    protected void onRefresh() {
        Project project = currentProject();
        String needle = search.trim().toLowerCase(Locale.ROOT);

        int total = project.getAttachments().size();
        int imageCount = 0;
        int textCount = 0;
        ArrayList<AttachmentLink> matches = new ArrayList<>();

        // Links stay in the order they were added.
        for (AttachmentLink link : project.getAttachments()) {
            CatalogItem item = AttachmentCatalog.findById(link.getCatalogId());
            boolean isImage = item != null && item.isImage();
            if (isImage) {
                imageCount++;
            } else {
                textCount++;
            }
            boolean typeMatches = typeFilter == TYPE_ALL
                    || (typeFilter == TYPE_IMAGES && isImage)
                    || (typeFilter == TYPE_TEXT && !isImage);
            String name = item != null ? item.getDisplayName() : "";
            boolean searchMatches = needle.isEmpty() || name.toLowerCase(Locale.ROOT).contains(needle);
            if (typeMatches && searchMatches) {
                matches.add(link);
            }
        }
        // A NEW list every time: ListAdapter + DiffUtil work out which rows changed.
        adapter.submitList(matches);

        chipAll.setText("All (" + total + ")");
        chipImages.setText("Images (" + imageCount + ")");
        chipText.setText("Text (" + textCount + ")");
        styleChip(chipAll, typeFilter == TYPE_ALL);
        styleChip(chipImages, typeFilter == TYPE_IMAGES);
        styleChip(chipText, typeFilter == TYPE_TEXT);

        tvShowing.setText("Showing " + matches.size() + " of " + total + " attachments");

        if (total == 0) {
            rvFiles.setVisibility(View.GONE);
            emptyNoAttachments.setVisibility(View.VISIBLE);
            emptyNoMatch.setVisibility(View.GONE);
        } else if (matches.isEmpty()) {
            rvFiles.setVisibility(View.GONE);
            emptyNoAttachments.setVisibility(View.GONE);
            emptyNoMatch.setVisibility(View.VISIBLE);
        } else {
            rvFiles.setVisibility(View.VISIBLE);
            emptyNoAttachments.setVisibility(View.GONE);
            emptyNoMatch.setVisibility(View.GONE);
        }
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(requireContext().getColor(selected ? R.color.card_white : R.color.text_primary));
    }

    // ---------------- row actions ----------------

    @Override
    public void onPreview(AttachmentLink link) {
        CatalogItem item = AttachmentCatalog.findById(link.getCatalogId());
        AttachmentPreviewDialog.show(requireContext(), link, item);
    }

    /** The remove icon on a row. */
    @Override
    public void onRemove(AttachmentLink link) {
        confirmRemoval(link);
    }

    /** Swiping a row to the right. */
    @Override
    public void onSwipedRight(AttachmentLink link) {
        confirmRemoval(link);
    }

    /**
     * Removes the link only after the typed name matches. If the user cancels (or closes the
     * dialog), the link stays and the row is put back where it was.
     */
    private void confirmRemoval(AttachmentLink link) {
        CatalogItem item = AttachmentCatalog.findById(link.getCatalogId());
        String name = item != null ? item.getDisplayName() : "";
        final String linkId = link.getId();
        RemoveAttachmentDialog.show(requireContext(), name,
                () -> {
                    currentProject().removeAttachment(linkId);
                    host.onProjectDataChanged();
                },
                () -> restoreRow(linkId));
    }

    /** Puts a swiped row back (rebinding it also resets its sideways offset). */
    private void restoreRow(String linkId) {
        int position = adapter.positionOfLink(linkId);
        if (position >= 0) {
            adapter.notifyItemChanged(position);
        }
    }
}
