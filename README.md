# TaskFlow (SMD Assignment 4)

Continues Assignment 3. Every `ListView` is now a **RecyclerView** with a custom adapter, the Tasks tab is one grouped
list with swipe and drag gestures, and the Files tab supports swipe-to-remove.

## What changed from Assignment 3

| Screen | Assignment 3 | Assignment 4 |
| --- | --- | --- |
| Home | ListView + `ProjectListAdapter` | RecyclerView + `ProjectAdapter` (`ListAdapter` + `DiffUtil`) |
| Tasks tab | ListView, flat list | **One** RecyclerView, one `TaskAdapter`, two view types (section header + task), grouped by status |
| Files tab | ListView + `AttachmentListAdapter` | RecyclerView + `AttachmentAdapter` (`ListAdapter` + `DiffUtil`) |

`notifyDataSetChanged()` is not used anywhere. Lists are updated with `submitList(newList)` (Home, Files) or with
`DiffUtil.calculateDiff` (Tasks), so only the rows that really changed are animated.

## Gestures

| Where | Gesture | Result |
| --- | --- | --- |
| Tasks, task row | Swipe **left** | Task becomes **Done** and moves to the Done group (already Done: toast "Task already completed", row restored) |
| Tasks, task row | Swipe **right** | "Delete task?" dialog. **Delete** removes it, **Cancel** (or tapping outside) puts the row back |
| Tasks, task row | Long-press, then drag | Reorder **inside the same group only** (never across a header or into another group) |
| Tasks, header row | any | Nothing: headers cannot be swiped or dragged |
| Files, attachment row | Swipe **right** | Type the attachment name to confirm. Wrong name shows an error; Cancel puts the row back |

## How the grouped list works

1. `TaskQuery` applies search, Status / Priority / Date filters and the sort.
2. `TaskGrouper` splits the result into groups in a fixed order: **Overdue, In Progress, To Do, Done**. Each task is in
   exactly one group (an unfinished past-due task is shown under Overdue). A group with no tasks gets no header.
3. `TaskAdapter` receives `TaskRow`s. `getItemViewType()` returns `TYPE_HEADER` or `TYPE_TASK`. A header shows the
   group name and its number of tasks.
4. A `TaskRow` is a **snapshot** of the task, so DiffUtil can tell what changed even though the `Task` is edited in place.

### Drag-to-reorder
`TaskTouchCallback` gives header rows no movement flags and refuses any drop onto a header or another group.
When a drag ends, the on-screen order is saved with `Project.applyTaskOrder(...)` and the Sort pill switches to
**Manual** so the list keeps exactly that order. Tasks hidden by a filter keep their own place.

## Data flow (unchanged from Assignment 3)
`HomeActivity` owns all projects and passes the clicked one to `ProjectActivity` as an intent extra. The fragments read the
one working project through `ProjectHost`. On Back the updated project returns in the result intent and Home replaces its
copy **by project id**, then submits a new list to the adapter.

## Manual test checklist

- [ ] Home: change something in a project, go back: only that row refreshes (progress bar, task count, attachments, description)
- [ ] Home: search still finds projects by name or tag, case-insensitive
- [ ] Tasks: one list with headers Overdue, In Progress, To Do, Done; each header shows its count
- [ ] Tasks: a group with no tasks (or none matching the filters) shows no header
- [ ] Swipe a To do task left: it moves to Done, header counts update
- [ ] Swipe a Done task left: "Task already completed", no duplicate, row snaps back
- [ ] Swipe a task right, press Cancel: row returns. Swipe again, press Delete: task disappears, counts update everywhere
- [ ] Long-press a task and drag it up/down inside its group: it moves; try to drag it over a header or into another group: it will not go
- [ ] After dragging, the Sort pill shows "Manual" and the order stays after switching tabs and back
- [ ] Headers cannot be swiped or dragged
- [ ] Files: swipe a row right, type a wrong name: error. Cancel: row returns. Swipe again, type the right name, Remove: it is gone, chip counts and Overview update
- [ ] Files: the Preview button and the remove icon still work
