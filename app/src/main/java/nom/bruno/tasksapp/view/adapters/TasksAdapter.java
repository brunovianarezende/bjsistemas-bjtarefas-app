package nom.bruno.tasksapp.view.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private RecyclerView mRecyclerView;

    public Observable<TasksAdapter.ViewHolder> onClickView() {
        return clickSubject;
    }

    public Observable<TasksAdapter.ViewHolder> onDeleteSingle() {
        return deleteSubject;
    }

    private void clearCurrentFocus() {
        ViewHolder currentlyFocused = getCurrentlyFocused();
        if (currentlyFocused != null) {
            currentlyFocused.hideButtons();
        }
    }

    private ViewHolder getCurrentlyFocused() {
        if (mState.hasFocus()) {
            return (ViewHolder) mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(mState.getFocusedTaskPosition()));
        } else {
            return null;
        }
    }


    public void updateTasks(List<Task> tasks) {
        AdapterState newState = new AdapterState();
        Task focusedTask = mState.getFocusedTask();
        newState.setTasks(tasks);
        if (newState.contains(focusedTask)) {
            newState.setFocusedTask(focusedTask);
        }

        mState = newState;
        notifyDataSetChanged();
    }

    public void focusOn(ViewHolder viewHolder) {
        Task relatedTask = mState.getTask(viewHolder.getAdapterPosition());

        if (!mState.hasFocus()) {
            viewHolder.showButtons();
            mState.setFocusedTask(relatedTask);
        } else if (!mState.isFocused(relatedTask)) {
            clearCurrentFocus();
            viewHolder.showButtons();
            mState.setFocusedTask(relatedTask);
        } else {
            clearCurrentFocus();
            mState.setFocusedTask(null);
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

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mState.getTask(position);

        holder.mTitleTextView.setText(task.getTitle());
        holder.mDescriptionTextView.setText(task.getDescription());
        if (mState.isFocused(task)) {
            holder.showButtons();
        } else {
            holder.hideButtons();
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;
        private TextView mDescriptionTextView;
        private ImageButton mDeleteButton;
        private ImageButton mEditButton;

        private ViewHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.item_task_title);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.item_task_description);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.item_task_delete);
            mEditButton = (ImageButton) itemView.findViewById(R.id.item_task_edit);
        }

        private void hideButtons() {
            mDeleteButton.setVisibility(View.GONE);
            mEditButton.setVisibility(View.GONE);
        }

        private void showButtons() {
            mDeleteButton.setVisibility(View.VISIBLE);
            mEditButton.setVisibility(View.VISIBLE);
        }

    }

    private static class AdapterState {
        private List<Task> mTasks = Collections.emptyList();
        private int mFocusedTaskId = -1;
        @SuppressLint("UseSparseArrays")
        private Map<Integer, Integer> mTaskId2Position = new HashMap<>();

        boolean hasFocus() {
            return mFocusedTaskId != -1;
        }

        void setFocusedTask(Task taskToFocus) {
            if (taskToFocus != null) {
                mFocusedTaskId = taskToFocus.getId();
            } else {
                mFocusedTaskId = -1;
            }
        }

        Task getFocusedTask() {
            if (mFocusedTaskId == -1) {
                return null;
            } else {
                return getTask(mTaskId2Position.get(mFocusedTaskId));
            }
        }

        boolean isFocused(Task task) {
            return mFocusedTaskId != -1 && task.equals(getFocusedTask());
        }

        Task getTask(int position) {
            return mTasks.get(position);
        }

        Integer getFocusedTaskPosition() {
            return mTaskId2Position.get(mFocusedTaskId);
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
