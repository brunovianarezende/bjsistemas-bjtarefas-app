package nom.bruno.tasksapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.view.adapters.TasksAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView rvTasks = (RecyclerView) findViewById(R.id.tasks_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTasks.getContext(),
                layoutManager.getOrientation());
        rvTasks.addItemDecoration(dividerItemDecoration);
        final TasksAdapter adapter = new TasksAdapter(this);
        rvTasks.setAdapter(adapter);
        rvTasks.setLayoutManager(layoutManager);

        new TaskService().getTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Task>>() {
                    @Override
                    public void accept(@NonNull List<Task> result) throws Exception {
                        adapter.setTasks(result);
                    }
                });

        adapter.onClickView()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TasksAdapter.ViewHolder>() {
                    @Override
                    public void accept(@NonNull TasksAdapter.ViewHolder viewHolder) throws Exception {
                        adapter.focusOn(viewHolder);
                    }
                });

        adapter.onDeleteSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TasksAdapter.ViewHolder>() {
                    @Override
                    public void accept(@NonNull TasksAdapter.ViewHolder viewHolder) throws Exception {
                        int position = rvTasks.getChildAdapterPosition(viewHolder.itemView);
                        adapter.deleteTask(viewHolder, position);
                    }
                });

    }
}
