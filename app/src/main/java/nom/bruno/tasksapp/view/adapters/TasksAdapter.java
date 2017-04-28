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
    private Context mContext;
    private List<Task> mTasks = new ArrayList<>();
    private PublishSubject<TasksAdapter.ViewHolder> clickSubject = PublishSubject.create();
    private PublishSubject<TasksAdapter.ViewHolder> deleteSubject = PublishSubject.create();
    private ViewHolder currentlyFocused = null;

    public TasksAdapter(Context context) {
        this.mContext = context;
    }

    public Observable<TasksAdapter.ViewHolder> onClickView() {
        return clickSubject;
    }

    public Observable<TasksAdapter.ViewHolder> onDeleteSingle() {
        return deleteSubject;
    }

    public void setTasks(List<Task> tasks) {
        this.mTasks = tasks;
        notifyDataSetChanged();
    }

    public void focusOn(ViewHolder viewHolder) {
        if (currentlyFocused == null) {
            viewHolder.showDeleteButton();
            currentlyFocused = viewHolder;
        } else if (currentlyFocused != viewHolder) {
            currentlyFocused.hideDeleteButton();
            viewHolder.showDeleteButton();
            currentlyFocused = viewHolder;
            currentlyFocused.hideDeleteButton();
        } else {
            currentlyFocused.hideDeleteButton();
            currentlyFocused = null;
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

    public void deleteTask(ViewHolder viewHolder, int position) {
        viewHolder.hideDeleteButton();
        mTasks.remove(position);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mTitleTextView;
        public final TextView mDescriptionTextView;
        public final ImageButton mDeleteButton;
        private boolean showingDeleteButton = true;

        public ViewHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.item_task_title);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.item_task_description);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.item_task_delete);
            hideDeleteButton();
        }

        public void toggleDeleteButton() {
            if (showingDeleteButton) {
                hideDeleteButton();
            } else {
                showDeleteButton();
            }
        }

        public void hideDeleteButton() {
            showingDeleteButton = false;
            mDeleteButton.setVisibility(View.GONE);
        }

        public void showDeleteButton() {
            showingDeleteButton = true;
            mDeleteButton.setVisibility(View.VISIBLE);
        }

    }
}
