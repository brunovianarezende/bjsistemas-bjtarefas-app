package nom.bruno.tasksapp.view.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.Utils;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskUpdate;
import nom.bruno.tasksapp.models.TaskUpdateParameters;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.ViewHolder> {
    private Activity mActivity;

    public static TasksAdapter initializeTasksAdapter(Activity activity) {
        RecyclerView rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTasks.getContext(),
                layoutManager.getOrientation());
        rvTasks.addItemDecoration(dividerItemDecoration);
        TasksAdapter adapter = new TasksAdapter(activity);
        adapter.bindRecyclerView(rvTasks);
        rvTasks.setLayoutManager(layoutManager);
        return adapter;
    }

    private TasksAdapter(Activity activity) {
        mActivity = activity;
    }

    private AdapterState mState = new AdapterState();

    private PublishSubject<Task> deleteSubject = PublishSubject.create();

    private PublishSubject<TaskUpdateParameters> saveSubject = PublishSubject.create();

    private PublishSubject<StateDelta> mStateChangeSubject = PublishSubject.create();

    private RecyclerView mRecyclerView;

    public Observable<Task> onDeleteSingle() {
        return deleteSubject;
    }

    public Observable<TaskUpdateParameters> onUpdate() {
        return saveSubject;
    }

    public Observable<StateDelta> onStateChange() {
        return mStateChangeSubject;
    }

    private ViewHolder getCurrentlySelected() {
        if (mState.hasTaskSelected()) {
            return getRelatedViewHolder(mState.getSelectedTask());
        } else {
            return null;
        }
    }

    private ViewHolder getRelatedViewHolder(Task task) {
        return (ViewHolder) mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(mState.getTaskPosition(task)));
    }

    public void updateTasks(List<Task> tasks) {
        AdapterState newState = new AdapterState();
        Task focusedTask = mState.getSelectedTask();
        newState.setTasks(tasks);
        if (newState.contains(focusedTask)) {
            newState.selectTask(focusedTask);
            newState.setAdapterState(mState.getAdapterState());
        }

        mState = newState;
        notifyDataSetChanged();
    }

    public List<Task> getMultipleSelectedTasks() {
        return mState.getMultipleSelected();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup recyclerView, int viewType) {
        Context context = recyclerView.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View taskView = inflater.inflate(R.layout.item_task, recyclerView, false);

        final ViewHolder viewHolder = new ViewHolder(taskView);

        RxView.clicks(viewHolder.mDeleteButton)
                .takeUntil(RxView.detaches(recyclerView))
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        switchToDeletingState(viewHolder);
                    }
                })
                .map(new Function<Object, Task>() {
                    @Override
                    public Task apply(@NonNull Object o) throws Exception {
                        return mState.getSelectedTask();
                    }
                })
                .subscribe(deleteSubject);

        RxView.clicks(viewHolder.mEditSaveButton)
                .takeUntil(RxView.detaches(recyclerView))
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        Utils.hideKeyboard(mActivity);
                        switchToSavingState(viewHolder);
                    }
                })
                .map(new Function<Object, TaskUpdateParameters>() {
                    @Override
                    public TaskUpdateParameters apply(@NonNull Object o) throws Exception {
                        TaskUpdateParameters update = new TaskUpdateParameters();
                        TaskUpdate taskUpdate = update.getUpdateData();
                        Task currentTask = mState.getSelectedTask();
                        update.setTaskId(currentTask.getId());
                        taskUpdate.setTitle(viewHolder.mTitleEditText.getText().toString());
                        taskUpdate.setDescription(viewHolder.mDescriptionEditText.getText().toString());
                        return update;
                    }
                })
                .subscribe(saveSubject);

        RxView.clicks(taskView)
                .takeUntil(RxView.detaches(recyclerView))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        Task relatedTask = mState.getTask(viewHolder.getAdapterPosition());

                        if (mState.getAdapterState() == States.SELECT_MULTIPLE_TASKS) {
                            auxHandleClickWhenInMultipleSelectState(viewHolder);
                        } else {
                            if (!mState.hasTaskSelected()) {
                                switchToItemSelectedState(viewHolder);
                            } else if (!mState.isSelected(relatedTask)) {
                                switchToItemSelectedState(viewHolder);
                            } else {
                                switchToViewState();
                            }
                        }
                    }
                });

        RxView.longClicks(taskView)
                .takeUntil(RxView.detaches(recyclerView))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Object o) throws Exception {
                        if (mState.getAdapterState() != States.SELECT_MULTIPLE_TASKS) {
                            switchToMultipleItemsSelectState(viewHolder);
                        } else {
                            auxHandleClickWhenInMultipleSelectState(viewHolder);
                        }
                    }
                });

        RxView.clicks(viewHolder.mEditButton)
                .takeUntil(RxView.detaches(recyclerView))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        switchToEditState(viewHolder);
                    }
                });

        RxView.clicks((viewHolder.mEditCancelButton))
                .takeUntil(RxView.detaches(recyclerView))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        Utils.hideKeyboard(mActivity);
                        viewHolder.showItemSelectedState();
                    }
                });

        viewHolder.showViewState();

        return viewHolder;
    }

    private void auxHandleClickWhenInMultipleSelectState(ViewHolder viewHolder) {
        Task relatedTask = mState.getTask(viewHolder.getAdapterPosition());
        if (mState.isOneOfTheSelected(relatedTask)) {
            mState.removeTaskFromMultipleSelected(relatedTask);
            viewHolder.showIsNotOneOfTheMultipleSelected();
            if (mState.getNumMultipleSelected() == 0) {
                switchToViewState();
            }
        } else {
            mState.addTaskToMultipleSelected(relatedTask);
            viewHolder.showIsOneOfTheMultipleSelected();
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mState.getTask(position);

        holder.mTitleTextView.setText(task.getTitle());
        holder.mDescriptionTextView.setText(task.getDescription());
        holder.mTitleEditText.setText(task.getTitle());
        holder.mDescriptionEditText.setText(task.getDescription());
        if (mState.getAdapterState() == States.SELECT_MULTIPLE_TASKS) {
            if (mState.isOneOfTheSelected(task)) {
                holder.showIsOneOfTheMultipleSelected();
            } else {
                holder.showViewState();
            }
        } else if (mState.isSelected(task)) {
            holder.showState(mState.getAdapterState());
        } else {
            holder.showViewState();
        }
    }

    @Override
    public int getItemCount() {
        return mState.getNumTasks();
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

    private void switchToEditState(ViewHolder viewHolder) {
        mState.selectTask(mState.getTask(viewHolder.getAdapterPosition()));
        changeState(States.EDIT_TASK);
        viewHolder.showEditState();
    }

    public void callMeWhenDeleteOperationIsFinished() {
        switchToViewState();
    }

    public void callMeIfDeleteOperationFailed() {
        switchToItemSelectedState(getCurrentlySelected());
    }

    public void callMeWhenUpdateOperationIsFinished() {
        switchToViewState();
    }

    public void callMeIfUpdateOperationFailed() {
        switchToEditState(getCurrentlySelected());
    }

    private void switchToViewState() {
        ViewHolder current = getCurrentlySelected();
        if (current != null) {
            current.showViewState();
        }
        mState.clearSelectedTask();
        mState.clearMultipleSelected();
        changeState(States.VIEW_TASK);
    }

    private void switchToItemSelectedState(ViewHolder viewHolder) {
        ViewHolder current = getCurrentlySelected();
        if (current != null) {
            current.showViewState();
        }
        mState.selectTask(mState.getTask(viewHolder.getAdapterPosition()));
        viewHolder.showItemSelectedState();
        changeState(States.TASK_SELECTED);
    }

    private void switchToSavingState(ViewHolder viewHolder) {
        changeState(States.SAVING_TASK);
        viewHolder.showSavingState();
    }

    private void switchToDeletingState(ViewHolder viewHolder) {
        viewHolder.showDeletingState();
        changeState(States.DELETING_TASK);
    }


    private void switchToMultipleItemsSelectState(ViewHolder viewHolder) {
        viewHolder.showSelectedEffect();
        ViewHolder current = getCurrentlySelected();
        if (current != null) {
            current.showViewState();
        }
        mState.clearSelectedTask();
        mState.addTaskToMultipleSelected(mState.getTask(viewHolder.getAdapterPosition()));
        changeState(States.SELECT_MULTIPLE_TASKS);
    }

    private void changeState(int newState) {
        StateDelta delta = new StateDelta(mState.getAdapterState(), newState);
        mState.setAdapterState(newState);
        mStateChangeSubject.onNext(delta);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout mLayout;
        private final ProgressBar mProgressBar;
        private final TextView mTitleTextView;
        private final TextView mDescriptionTextView;
        private final ImageButton mDeleteButton;
        private final ImageButton mEditButton;
        private final ImageButton mEditSaveButton;
        private final ImageButton mEditCancelButton;
        private final EditText mTitleEditText;
        private final EditText mDescriptionEditText;

        private final List<View> allItems;

        private ViewHolder(View itemView) {
            super(itemView);

            mLayout = (ConstraintLayout) itemView.findViewById(R.id.item_task_layout);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.item_task_progress_bar);
            mTitleTextView = (TextView) itemView.findViewById(R.id.item_task_title);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.item_task_description);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.item_task_delete);
            mEditButton = (ImageButton) itemView.findViewById(R.id.item_task_edit);
            mEditSaveButton = (ImageButton) itemView.findViewById(R.id.item_task_save_edit);
            mEditCancelButton = (ImageButton) itemView.findViewById(R.id.item_task_cancel_edit);
            mTitleEditText = (EditText) itemView.findViewById(R.id.item_task_edit_title);
            mDescriptionEditText = (EditText) itemView.findViewById(R.id.item_task_edit_description);

            allItems = Arrays.asList(mProgressBar, mTitleTextView, mDescriptionTextView, mDeleteButton,
                    mEditButton, mTitleEditText, mDescriptionEditText, mEditSaveButton,
                    mEditCancelButton);
        }

        void showSelectedEffect() {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final RippleDrawable rd = (RippleDrawable) ContextCompat.getDrawable(itemView.getContext(), R.drawable.item_selected_ripple);
                mLayout.setBackground(rd);
                final float centreX = itemView.getWidth() / 2;
                final float centreY = itemView.getHeight() / 2;
                rd.setHotspot(centreX, centreY);
                rd.setState(new int[]{});
            } else {
                mLayout.setSelected(true);
            }
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
            showDefaultBackground();
            showOnly(mTitleTextView, mDescriptionTextView);
        }

        private void showItemSelectedState() {
            showDefaultBackground();
            showOnly(mTitleTextView, mDescriptionTextView, mDeleteButton, mEditButton);
        }

        private void showEditState() {
            showDefaultBackground();
            showOnly(mTitleEditText, mDescriptionEditText, mEditSaveButton, mEditCancelButton);
        }

        private void showSavingState() {
            showDefaultBackground();
            showOnly(mProgressBar);
        }

        private void showDeletingState() {
            showDefaultBackground();
            showOnly(mProgressBar);
        }

        private void showState(int state) {
            switch (state) {
                case States.VIEW_TASK:
                    showViewState();
                    break;
                case States.TASK_SELECTED:
                    showItemSelectedState();
                    break;
                case States.EDIT_TASK:
                    showEditState();
                    break;
                case States.SAVING_TASK:
                    showSavingState();
                    break;
                case States.DELETING_TASK:
                    showDeletingState();
                    break;
            }
        }

        void showDefaultBackground() {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mLayout.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.item_task_press_events));
            } else {
                mLayout.setSelected(false);
            }
        }

        void showIsNotOneOfTheMultipleSelected() {
            mLayout.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
        }

        void showIsOneOfTheMultipleSelected() {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mLayout.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorSelectedBackground));
            } else {
                mLayout.setSelected(true);
            }
        }
    }

    public interface States {
        int VIEW_TASK = 0;
        int TASK_SELECTED = 1;
        int EDIT_TASK = 2;
        int SAVING_TASK = 3;
        int DELETING_TASK = 4;
        int SELECT_MULTIPLE_TASKS = 4;
    }

    public static class StateDelta {
        final private int previous;
        final private int current;

        StateDelta(int previous, int current) {
            this.previous = previous;
            this.current = current;
        }

        public int getPrevious() {
            return previous;
        }

        public int getCurrent() {
            return current;
        }
    }

    private static class AdapterState {
        private List<Task> mTasks = Collections.emptyList();
        private int mSelectedTaskId = -1;
        private int mAdapterState = States.VIEW_TASK;
        @SuppressLint("UseSparseArrays")
        private Map<Integer, Boolean> mMultipleSelectedTasks = new HashMap<>();
        @SuppressLint("UseSparseArrays")
        private Map<Integer, Integer> mTaskId2Position = new HashMap<>();

        boolean hasTaskSelected() {
            return mSelectedTaskId != -1;
        }

        void selectTask(@NonNull Task taskToFocus) {
            mSelectedTaskId = taskToFocus.getId();
        }

        void clearSelectedTask() {
            mSelectedTaskId = -1;
        }

        Task getSelectedTask() {
            if (mSelectedTaskId == -1) {
                return null;
            } else {
                return getTaskById(mSelectedTaskId);
            }
        }

        Integer getTaskPosition(Task task) {
            if (contains(task)) {
                return mTaskId2Position.get(task.getId());
            } else {
                return null;
            }
        }

        boolean isSelected(Task task) {
            return mSelectedTaskId != -1 && task.equals(getSelectedTask());
        }

        int getAdapterState() {
            return mAdapterState;
        }

        void setAdapterState(int state) {
            mAdapterState = state;
        }

        Task getTask(int position) {
            return mTasks.get(position);
        }

        private Task getTaskById(int id) {
            return getTask(mTaskId2Position.get(id));
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

        void addTaskToMultipleSelected(Task task) {
            mMultipleSelectedTasks.put(task.getId(), true);
        }

        boolean isOneOfTheSelected(Task task) {
            return mMultipleSelectedTasks.containsKey(task.getId());
        }

        void removeTaskFromMultipleSelected(Task task) {
            mMultipleSelectedTasks.remove(task.getId());
        }

        int getNumMultipleSelected() {
            return mMultipleSelectedTasks.size();
        }

        void clearMultipleSelected() {
            mMultipleSelectedTasks.clear();
        }

        List<Task> getMultipleSelected() {
            List<Task> result = new ArrayList<>();
            for (Integer taskId : mMultipleSelectedTasks.keySet()) {
                result.add(getTaskById(taskId));
            }
            return result;
        }
    }
}
