package com.example.taskflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.adapters.ProjectAdapter;
import com.example.taskflow.data.AttachmentCatalog;
import com.example.taskflow.data.Project;
import com.example.taskflow.data.SampleData;
import com.example.taskflow.ui.ProjectDialogHelper;
import com.example.taskflow.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * Home screen. It owns the FULL project collection (allProjects). The RecyclerView shows
 * allProjects filtered by the search text. Every change is sent to the adapter as a NEW list
 * with submitList(); ListAdapter + DiffUtil then refresh only the rows that changed.
 * Projects are always found by their unique id, never by name or row position.
 */
public class HomeActivity extends AppCompatActivity {

    private static final String STATE_PROJECTS = "state_projects";

    private final ArrayList<Project> allProjects = new ArrayList<>();
    private ProjectAdapter adapter;
    private String searchQuery = "";

    private TextView tvShowingCount;
    private TextView tvProjectsEmpty;
    private RecyclerView rvProjects;

    private ActivityResultLauncher<Intent> projectLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        registerProjectLauncher();
        loadProjects(savedInstanceState);
        initViews();
        showProjects(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_PROJECTS, allProjects);
    }

    private void loadProjects(Bundle savedInstanceState) {
        ArrayList<Project> restored = null;
        if (savedInstanceState != null) {
            restored = savedInstanceState.getParcelableArrayList(STATE_PROJECTS);
        }
        if (restored != null) {
            allProjects.addAll(restored);
        } else {
            allProjects.addAll(SampleData.createProjects());
        }
    }

    private void initViews() {
        TextView tvWelcome = findViewById(R.id.tvWelcomeMessage);
        tvWelcome.setText(getString(R.string.welcome_message, "Fahad"));

        tvShowingCount = findViewById(R.id.tvShowingProjects);
        tvProjectsEmpty = findViewById(R.id.tvProjectsEmpty);
        rvProjects = findViewById(R.id.rvProjects);

        // Clicking a row opens that project. The Project object is handed over through an
        // intent extra; Home later finds it again by its id.
        adapter = new ProjectAdapter(this::openProject);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etProjectSearch);
        etSearch.addTextChangedListener(new SimpleTextWatcher(text -> {
            searchQuery = text;
            showProjects(null);
        }));

        findViewById(R.id.btnNewProject).setOnClickListener(v -> showNewProjectDialog());
    }

    private void openProject(Project project) {
        Intent intent = new Intent(HomeActivity.this, ProjectActivity.class);
        intent.putExtra(ProjectActivity.EXTRA_PROJECT, project);
        projectLauncher.launch(intent);
    }

    private void showNewProjectDialog() {
        ProjectDialogHelper.show(this, null, (name, tag, description, attachment) -> {
            // A new project: its own unique id, no tasks, no comments, 0/0 and 0%.
            Project project = new Project(UUID.randomUUID().toString(), name, tag, description, 0);
            if (attachment != null) {
                project.addAttachment(AttachmentCatalog.createLink(attachment));
            }
            allProjects.add(project); // appended at the end
            showProjects(project.getId());
        });
    }

    /**
     * Applies the search to allProjects and submits the result as a NEW list.
     * Search is case-insensitive and matches any part of the project name or tag.
     *
     * @param scrollToProjectId if not null, scroll to that project once the list is updated
     */
    private void showProjects(final String scrollToProjectId) {
        String needle = searchQuery.trim().toLowerCase(Locale.ROOT);
        ArrayList<Project> filtered = new ArrayList<>();
        for (Project project : allProjects) {
            boolean matches = needle.isEmpty()
                    || project.getName().toLowerCase(Locale.ROOT).contains(needle)
                    || project.getTag().toLowerCase(Locale.ROOT).contains(needle);
            if (matches) {
                filtered.add(project);
            }
        }

        // ListAdapter ignores a list it already has, so always hand it a new ArrayList.
        final int scrollPosition = indexOfProject(filtered, scrollToProjectId);
        adapter.submitList(filtered, new Runnable() {
            @Override
            public void run() {
                if (scrollPosition >= 0) {
                    rvProjects.scrollToPosition(scrollPosition);
                }
            }
        });
        tvProjectsEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        int total = allProjects.size();
        if (needle.isEmpty()) {
            tvShowingCount.setText("Showing " + total + (total == 1 ? " project" : " projects"));
        } else {
            tvShowingCount.setText("Showing " + filtered.size() + " of " + total + " projects");
        }
    }

    private static int indexOfProject(ArrayList<Project> projects, String projectId) {
        if (projectId == null) {
            return -1;
        }
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId().equals(projectId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * ProjectActivity sends back its (possibly changed) project. The matching Home project is
     * replaced BY ID, then a new list is submitted so only the changed row is refreshed.
     */
    private void registerProjectLauncher() {
        projectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data == null) {
                        return;
                    }
                    Project updated = data.getParcelableExtra(ProjectActivity.EXTRA_PROJECT);
                    if (updated == null) {
                        return;
                    }
                    for (int i = 0; i < allProjects.size(); i++) {
                        if (allProjects.get(i).getId().equals(updated.getId())) {
                            allProjects.set(i, updated);
                            break;
                        }
                    }
                    showProjects(null);
                });
    }
}
