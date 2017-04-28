package nom.bruno.tasksapp.view.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private PublishSubject<View> clickSubject = PublishSubject.create();

    public TasksAdapter(Context context) {
        this.mContext = context;
    }

    public Observable<View> onClickView() {
        return clickSubject;
    }

    public void setTasks(List<Task> tasks) {
        this.mTasks = tasks;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup recyclerView, int viewType) {
        Context context = recyclerView.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        final View taskView = inflater.inflate(R.layout.item_task, recyclerView, false);

        RxView.clicks(taskView)
                .takeUntil(RxView.detaches(recyclerView))
                .map(new Function<Object, View>() {
                    @Override
                    public View apply(@NonNull Object o) throws Exception {
                        return taskView;
                    }
                })
                .subscribe(clickSubject);

        ViewHolder viewHolder = new ViewHolder(taskView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mTasks.get(position);

        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public Task getTask(int position) {
        return mTasks.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView descriptionTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.item_task_title);
            descriptionTextView = (TextView) itemView.findViewById(R.id.item_task_description);
        }
    }
}
