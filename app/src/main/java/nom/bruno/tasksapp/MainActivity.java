package nom.bruno.tasksapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import nom.bruno.tasksapp.model.Task;
import nom.bruno.tasksapp.view.adapters.TasksAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rvTasks = (RecyclerView) findViewById(R.id.tasks_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTasks.getContext(),
                layoutManager.getOrientation());
        rvTasks.addItemDecoration(dividerItemDecoration);
        List<Task> tasks = new ArrayList<>();
        Task t1 = new Task();
        t1.setTitle("Title 1");
        t1.setDescription("Description 1");
        tasks.add(t1);
        Task t2 = new Task();
        t2.setTitle("Title 2");
        t2.setDescription("Description 2");
        tasks.add(t2);
        TasksAdapter adapter = new TasksAdapter(this, tasks);
        rvTasks.setAdapter(adapter);
        rvTasks.setLayoutManager(layoutManager);
    }
}
