package nom.bruno.tasksapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.view.adapters.TasksAdapter;

public class MainActivity extends AppCompatActivity {
    private TasksAdapter mAdapter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView rvTasks = (RecyclerView) findViewById(R.id.tasks_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTasks.getContext(),
                layoutManager.getOrientation());
        rvTasks.addItemDecoration(dividerItemDecoration);
        mAdapter = new TasksAdapter();
        mAdapter.bindRecyclerView(rvTasks);
        rvTasks.setLayoutManager(layoutManager);

        final TaskService ts = new TaskService();

        if (savedInstanceState == null) {
            ts.getTasks()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<List<Task>>() {
                        @Override
                        public void accept(@NonNull List<Task> result) throws Exception {
                            mAdapter.updateTasks(result);
                        }
                    });
        } else {
            mAdapter.deserializeState(savedInstanceState.getString(mAdapter.getClass().getCanonicalName()));
        }

        Observable.interval(1, TimeUnit.MINUTES)
                .flatMap(new Function<Long, Observable<List<Task>>>() {
                    @Override
                    public Observable<List<Task>> apply(@NonNull Long aLong) throws Exception {
                        return ts.getTasks();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Task>>() {
                    @Override
                    public void accept(@NonNull List<Task> tasks) throws Exception {
                        mAdapter.updateTasks(tasks);
                    }
                });

        mAdapter.onDeleteSingle()
                .observeOn(Schedulers.io())
                .flatMap(new Function<TasksAdapter.ViewHolder, Observable<MyVoid>>() {
                    @Override
                    public Observable<MyVoid> apply(@NonNull TasksAdapter.ViewHolder viewHolder) throws Exception {
                        int position = rvTasks.getChildAdapterPosition(viewHolder.itemView);
                        Task task = mAdapter.getTask(position);
                        return ts.deleteTask(task.getId());
                    }
                })
                .flatMap(new Function<MyVoid, Observable<List<Task>>>() {
                    @Override
                    public Observable<List<Task>> apply(@NonNull MyVoid aVoid) throws Exception {
                        return ts.getTasks();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Task>>() {
                    @Override
                    public void accept(@NonNull List<Task> tasks) throws Exception {
                        mAdapter.updateTasks(tasks);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(mAdapter.getClass().getCanonicalName(), mAdapter.serializeState());
    }
}
