package nom.bruno.tasksapp.view.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.models.Task;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.ViewHolder> {
    private List<Task> mTasks = new ArrayList<>();
    private PublishSubject<TasksAdapter.ViewHolder> clickSubject = PublishSubject.create();
    private PublishSubject<TasksAdapter.ViewHolder> deleteSubject = PublishSubject.create();
    private ViewHolder currentlyFocused = null;

    public TasksAdapter(Context context) {
    }

    public Observable<TasksAdapter.ViewHolder> onClickView() {
        return clickSubject;
    }

    public Observable<TasksAdapter.ViewHolder> onDeleteSingle() {
        return deleteSubject;
    }

    private void clearCurrentFocus() {
        if (currentlyFocused != null) {
            currentlyFocused.hideDeleteButton();
            currentlyFocused = null;
        }
    }

    public void setTasks(List<Task> tasks) {
        this.mTasks = tasks;
        clearCurrentFocus();
        notifyDataSetChanged();
    }

    public void focusOn(ViewHolder viewHolder) {
        if (currentlyFocused == null) {
            viewHolder.showDeleteButton();
            currentlyFocused = viewHolder;
        } else if (currentlyFocused != viewHolder) {
            clearCurrentFocus();
            viewHolder.showDeleteButton();
            currentlyFocused = viewHolder;
        } else {
            clearCurrentFocus();
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
        Task task = mTasks.get(position);

        holder.mTitleTextView.setText(task.getTitle());
        holder.mDescriptionTextView.setText(task.getDescription());
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public Task getTask(int position) {
        return mTasks.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;
        private TextView mDescriptionTextView;
        private ImageButton mDeleteButton;

        private ViewHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.item_task_title);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.item_task_description);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.item_task_delete);
            hideDeleteButton();
        }

        private void hideDeleteButton() {
            mDeleteButton.setVisibility(View.GONE);
        }

        private void showDeleteButton() {
            mDeleteButton.setVisibility(View.VISIBLE);
        }

    }
}
