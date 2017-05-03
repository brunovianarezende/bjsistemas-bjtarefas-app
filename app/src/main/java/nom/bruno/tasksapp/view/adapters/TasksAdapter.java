package nom.bruno.tasksapp.view.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.models.Task;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.ViewHolder> {
    private AdapterState mState = new AdapterState();

    private PublishSubject<TasksAdapter.ViewHolder> clickSubject = PublishSubject.create();
    private PublishSubject<TasksAdapter.ViewHolder> deleteSubject = PublishSubject.create();
    private PublishSubject<TasksAdapter.ViewHolder> editSubject = PublishSubject.create();

    private RecyclerView mRecyclerView;

    public Observable<TasksAdapter.ViewHolder> onClickView() {
        return clickSubject;
    }

    public Observable<TasksAdapter.ViewHolder> onDeleteSingle() {
        return deleteSubject;
    }

    public Observable<TasksAdapter.ViewHolder> onEditSingle() {
        return editSubject;
    }

    private void clearCurrentlySelected() {
        ViewHolder currentlyFocused = getCurrentlySelected();
        if (currentlyFocused != null) {
            currentlyFocused.showViewState();
        }
    }

    private ViewHolder getCurrentlySelected() {
        if (mState.hasTaskSelected()) {
            return (ViewHolder) mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(mState.getSelectedTaskPosition()));
        } else {
            return null;
        }
    }


    public void updateTasks(List<Task> tasks) {
        AdapterState newState = new AdapterState();
        Task focusedTask = mState.getSelectedTask();
        newState.setTasks(tasks);
        if (newState.contains(focusedTask)) {
            newState.selectTask(focusedTask);
            newState.setSelectedTaskState(mState.getSelectedTaskState());
        }

        mState = newState;
        notifyDataSetChanged();
    }

    public void focusOn(ViewHolder viewHolder) {
        Task relatedTask = mState.getTask(viewHolder.getAdapterPosition());

        if (!mState.hasTaskSelected()) {
            viewHolder.showItemSelectedState();
            mState.selectTask(relatedTask);
        } else if (!mState.isSelected(relatedTask)) {
            clearCurrentlySelected();
            viewHolder.showItemSelectedState();
            mState.selectTask(relatedTask);
        } else {
            clearCurrentlySelected();
            mState.selectTask(null);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup recyclerView, int viewType) {
        Context context = recyclerView.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View taskView = inflater.inflate(R.layout.item_task, recyclerView, false);
        final ViewHolder viewHolder = new ViewHolder(taskView);

        RxView.clicks(taskView)
                .takeUntil(RxView.detaches(recyclerView))
                .map(new Function<Object, TasksAdapter.ViewHolder>() {
                    @Override
                    public TasksAdapter.ViewHolder apply(@NonNull Object o) throws Exception {
                        return viewHolder;
                    }
                })
                .subscribe(clickSubject);

        RxView.clicks(viewHolder.mDeleteButton)
                .takeUntil(RxView.detaches(recyclerView))
                .map(new Function<Object, TasksAdapter.ViewHolder>() {
                    @Override
                    public TasksAdapter.ViewHolder apply(@NonNull Object o) throws Exception {
                        return viewHolder;
                    }
                })
                .subscribe(deleteSubject);

        RxView.clicks(viewHolder.mEditButton)
                .takeUntil(RxView.detaches(recyclerView))
                .map(new Function<Object, TasksAdapter.ViewHolder>() {
                    @Override
                    public TasksAdapter.ViewHolder apply(@NonNull Object o) throws Exception {
                        return viewHolder;
                    }
                })
                .subscribe(editSubject);

        startViewMode(viewHolder);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mState.getTask(position);

        holder.mTitleTextView.setText(task.getTitle());
        holder.mDescriptionTextView.setText(task.getDescription());
        holder.mTitleEditText.setText(task.getTitle());
        holder.mDescriptionEditText.setText(task.getDescription());
        if (mState.isSelected(task)) {
            holder.showState(mState.getSelectedTaskState());
        } else {
            holder.showViewState();
        }
    }

    @Override
    public int getItemCount() {
        return mState.getNumTasks();
    }

    public Task getTask(int position) {
        return mState.getTask(position);
    }

    public String serializeState() {
        Gson gson = new Gson();
        return gson.toJson(mState);
    }

    public void deserializeState(String serialized) {
        Gson gson = new Gson();
        mState = gson.fromJson(serialized, AdapterState.class);
        notifyDataSetChanged();
    }

    public void bindRecyclerView(RecyclerView recyclerView) {
        recyclerView.setAdapter(this);
        this.mRecyclerView = recyclerView;
    }

    public void startEditMode(ViewHolder viewHolder) {
        viewHolder.showEditState();
        mState.setSelectedTaskState(EDIT_TASK);
    }

    private void startViewMode(ViewHolder viewHolder) {
        viewHolder.showViewState();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;
        private TextView mDescriptionTextView;
        private ImageButton mDeleteButton;
        private ImageButton mEditButton;
        private EditText mTitleEditText;
        private EditText mDescriptionEditText;

        private final List<View> allItems;

        private ViewHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.item_task_title);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.item_task_description);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.item_task_delete);
            mEditButton = (ImageButton) itemView.findViewById(R.id.item_task_edit);
            mTitleEditText = (EditText) itemView.findViewById(R.id.item_task_edit_title);
            mDescriptionEditText = (EditText) itemView.findViewById(R.id.item_task_edit_description);

            allItems = Arrays.asList(mTitleTextView, mDescriptionTextView, mDeleteButton,
                    mEditButton, mTitleEditText, mDescriptionEditText);
        }

        private void showOnly(View... items) {
            Set<Integer> idsToShow = new HashSet<>();
            for (View item : items) {
                idsToShow.add(item.getId());
            }
            for (View item : allItems) {
                if (idsToShow.contains(item.getId())) {
                    item.setVisibility(View.VISIBLE);
                } else {
                    item.setVisibility(View.GONE);
                }
            }
        }

        private void showViewState() {
            showOnly(mTitleTextView, mDescriptionTextView);
        }

        private void showItemSelectedState() {
            showOnly(mTitleTextView, mDescriptionTextView, mDeleteButton, mEditButton);
        }

        private void showEditState() {
            showOnly(mTitleEditText, mDescriptionEditText);
        }

        private void showState(int state) {
            switch (state) {
                case VIEW_TASK:
                    showViewState();
                    break;
                case TASK_SELECTED:
                    showItemSelectedState();
                    break;
                case EDIT_TASK:
                    showEditState();
                    break;
            }
        }
    }

    private static final int VIEW_TASK = 0;
    private static final int TASK_SELECTED = 1;
    private static final int EDIT_TASK = 2;

    private static class AdapterState {
        private List<Task> mTasks = Collections.emptyList();
        private int mSelectedTaskId = -1;
        private int mSelectedTaskState = VIEW_TASK;
        @SuppressLint("UseSparseArrays")
        private Map<Integer, Integer> mTaskId2Position = new HashMap<>();

        boolean hasTaskSelected() {
            return mSelectedTaskId != -1;
        }

        void selectTask(Task taskToFocus) {
            if (taskToFocus != null) {
                mSelectedTaskId = taskToFocus.getId();
                mSelectedTaskState = TASK_SELECTED;
            } else {
                mSelectedTaskId = -1;
                mSelectedTaskState = VIEW_TASK;
            }
        }

        Task getSelectedTask() {
            if (mSelectedTaskId == -1) {
                return null;
            } else {
                return getTask(mTaskId2Position.get(mSelectedTaskId));
            }
        }

        boolean isSelected(Task task) {
            return mSelectedTaskId != -1 && task.equals(getSelectedTask());
        }

        Integer getSelectedTaskPosition() {
            return mTaskId2Position.get(mSelectedTaskId);
        }

        int getSelectedTaskState() {
            return mSelectedTaskState;
        }

        void setSelectedTaskState(int state) {
            mSelectedTaskState = state;
        }

        Task getTask(int position) {
            return mTasks.get(position);
        }

        int getNumTasks() {
            return mTasks.size();
        }

        void setTasks(List<Task> tasks) {
            mTasks = tasks;
            for (int i = 0; i < mTasks.size(); i++) {
                mTaskId2Position.put(mTasks.get(i).getId(), i);
            }
        }

        boolean contains(Task task) {
            return task != null && mTaskId2Position.containsKey(task.getId());
        }
    }
}
