package nom.bruno.tasksapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.models.TaskUpdateParameters;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.view.adapters.TasksAdapter;

public class MainActivity extends AppCompatActivity {
    private TasksAdapter mAdapter = null;
    private ActivityState mState = new ActivityState();
    private PublishSubject<Object> mAddTaskSubject = PublishSubject.create();
    private AddTaskView mAddTaskView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddTaskView = new AddTaskView(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tasks_toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorToolbarTitle));
        setSupportActionBar(toolbar);

        final RecyclerView rvTasks = (RecyclerView) findViewById(R.id.tasks_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTasks.getContext(),
                layoutManager.getOrientation());
        rvTasks.addItemDecoration(dividerItemDecoration);
        mAdapter = new TasksAdapter(this);
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
            switchToDefaultState();
        } else {
            deserializeState(savedInstanceState.getString(getClass().getCanonicalName()));
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
                .flatMap(new Function<Task, Observable<MyVoid>>() {
                    @Override
                    public Observable<MyVoid> apply(@NonNull Task task) throws Exception {
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

        mAdapter.onUpdate()
                .observeOn(Schedulers.io())
                .flatMap(new Function<TaskUpdateParameters, Observable<MyVoid>>() {
                    @Override
                    public Observable<MyVoid> apply(@NonNull TaskUpdateParameters updateDescription) throws Exception {
                        return ts.updateTask(updateDescription.getTaskId(), updateDescription.getUpdateData());
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

        mAddTaskSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        switchToAddTaskState();
                    }
                });

        mAddTaskView.newTaskData()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<TaskCreation>() {
                    @Override
                    public void accept(@NonNull TaskCreation taskCreation) throws Exception {
                        switchToDefaultState();
                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Function<TaskCreation, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(@NonNull TaskCreation taskCreation) throws Exception {
                        return ts.addTask(taskCreation);
                    }
                })
                .flatMap(new Function<Integer, Observable<List<Task>>>() {
                    @Override
                    public Observable<List<Task>> apply(@NonNull Integer newId) throws Exception {
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

        mAddTaskView.getCancelAddNewTaskClicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        switchToDefaultState();
                    }
                });
    }

    private void hideKeyboard() {
        Utils.hideKeyboard(this);
    }

    private void switchToDefaultState() {
        mState.setState(DEFAULT_STATE);
        hideKeyboard();
        mAddTaskView.hideAllFields();
    }

    private void switchToAddTaskState() {
        mState.setState(ADD_TASK_STATE);
        mAddTaskView.cleanFields();
        mAddTaskView.showAllFields();
    }

    private void switchToState(int state) {
        switch (state) {
            case DEFAULT_STATE:
                switchToDefaultState();
                break;
            case ADD_TASK_STATE:
                switchToAddTaskState();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getClass().getCanonicalName(), serializeState());
        outState.putString(mAdapter.getClass().getCanonicalName(), mAdapter.serializeState());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tasks_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_task:
                mAddTaskSubject.onNext("");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deserializeState(String serializedState) {
        Gson gson = new Gson();
        mState = gson.fromJson(serializedState, ActivityState.class);
        switchToState(mState.getState());
    }

    private String serializeState() {
        Gson gson = new Gson();
        return gson.toJson(mState);
    }

    private static class AddTaskView {
        private final EditText mTitleEditText;
        private final EditText mDescriptionEditText;
        private final ImageButton mAddSaveButton;
        private final ImageButton mAddCancelButton;
        private final List<View> allItems;
        private final Observable<Object> mSaveNewTaskClicks;
        private final Observable<Object> mCancelAddNewTaskClicks;

        public AddTaskView(Activity activity) {
            mTitleEditText = (EditText) activity.findViewById(R.id.tasks_add_task_title);
            mDescriptionEditText = (EditText) activity.findViewById(R.id.tasks_add_task_description);
            mAddSaveButton = (ImageButton) activity.findViewById(R.id.tasks_add_task_save);
            mAddCancelButton = (ImageButton) activity.findViewById(R.id.tasks_add_task_cancel);
            allItems = Arrays.asList(mTitleEditText, mDescriptionEditText, mAddSaveButton, mAddCancelButton);

            mSaveNewTaskClicks = RxView.clicks(mAddSaveButton);
            mCancelAddNewTaskClicks = RxView.clicks(mAddCancelButton);
        }

        public Observable<TaskCreation> newTaskData() {
            return mSaveNewTaskClicks.map(new Function<Object, TaskCreation>() {
                @Override
                public TaskCreation apply(@io.reactivex.annotations.NonNull Object o) throws Exception {
                    return getNewTaskData();
                }
            });
        }

        public Observable<Object> getCancelAddNewTaskClicks() {
            return mCancelAddNewTaskClicks;
        }

        void showAllFields() {
            for (View view : allItems) {
                view.setVisibility(View.VISIBLE);
            }
        }

        void hideAllFields() {
            for (View view : allItems) {
                view.setVisibility(View.GONE);
            }
        }

        public void cleanFields() {
            mTitleEditText.setText("");
            mDescriptionEditText.setText("");
        }

        public TaskCreation getNewTaskData() {
            TaskCreation taskData = new TaskCreation();
            taskData.setTitle(mTitleEditText.getText().toString());
            taskData.setDescription(mDescriptionEditText.getText().toString());
            return taskData;
        }
    }

    private static final int DEFAULT_STATE = 0;
    private static final int ADD_TASK_STATE = 1;

    private static class ActivityState {
        private int mState;

        int getState() {
            return mState;
        }

        void setState(int mState) {
            this.mState = mState;
        }
    }
}
