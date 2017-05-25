package nom.bruno.tasksapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import nom.bruno.tasksapp.ExceptionHandler;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.Utils;
import nom.bruno.tasksapp.androidservices.NotificationService;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.models.TaskUpdateParameters;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.view.adapters.TasksAdapter;

public class MainActivity extends AppCompatActivity {
    private TasksAdapter mAdapter = null;
    private ActivityState mState = new ActivityState();
    private PublishSubject<Object> mUpdateTasksSubject = PublishSubject.create();
    private PublishSubject<Object> mActivityIsDestroyed = PublishSubject.create();
    private IconBar mIconBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // add exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        // hack to start notification service if it wasn't started on boot
        NotificationService.scheduleJob(this);

        setContentView(R.layout.activity_main);

        mIconBar = IconBar.initializeToolbar(this);

        final RecyclerView rvTasks = (RecyclerView) findViewById(R.id.tasks_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTasks.getContext(),
                layoutManager.getOrientation());
        rvTasks.addItemDecoration(dividerItemDecoration);
        mAdapter = new TasksAdapter(this);
        mAdapter.bindRecyclerView(rvTasks);
        rvTasks.setLayoutManager(layoutManager);

        final TaskService ts = TaskService.getInstance(this);

        if (savedInstanceState == null) {
            List<Task> tasks = ts.getPersistedTasks();
            mAdapter.updateTasks(tasks);
            ts.getTasks()
                    .onErrorReturnItem(tasks)
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

        updateScreenIn30SecondsIfItIsntUpdatedBefore();

        mAdapter.onDeleteSingle()
                .observeOn(Schedulers.io())
                .flatMap(new Function<Task, Observable<MyVoid>>() {
                    @Override
                    public Observable<MyVoid> apply(@NonNull Task task) throws Exception {
                        return ts
                                .deleteTask(task.getId())
                                .observeOn(AndroidSchedulers.mainThread())
                                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends MyVoid>>() {
                                    @Override
                                    public ObservableSource<? extends MyVoid> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                        mAdapter.callMeIfDeleteOperationFailed();
                                        showServerError();
                                        return Observable.empty();
                                    }
                                });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<MyVoid>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull MyVoid myVoid) throws Exception {
                        mAdapter.callMeWhenDeleteOperationIsFinished();
                    }
                })
                .subscribe(mUpdateTasksSubject);

        mAdapter.onUpdate()
                .observeOn(Schedulers.io())
                .flatMap(new Function<TaskUpdateParameters, Observable<MyVoid>>() {
                    @Override
                    public Observable<MyVoid> apply(@NonNull TaskUpdateParameters updateDescription) throws Exception {
                        return ts.
                                updateTask(updateDescription.getTaskId(), updateDescription.getUpdateData())
                                .observeOn(AndroidSchedulers.mainThread())
                                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends MyVoid>>() {
                                    @Override
                                    public ObservableSource<? extends MyVoid> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                        mAdapter.callMeIfUpdateOperationFailed();
                                        showServerError();
                                        return Observable.empty();
                                    }
                                });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<MyVoid>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull MyVoid myVoid) throws Exception {
                        mAdapter.callMeWhenUpdateOperationIsFinished();
                    }
                })
                .subscribe(mUpdateTasksSubject);

        mAdapter.onStateChange()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TasksAdapter.StateDelta>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull TasksAdapter.StateDelta delta) throws Exception {
                        if (delta.getCurrent() == TasksAdapter.States.SELECT_MULTIPLE_TASKS) {
                            switchToSelectMultipleItemsState();
                        } else if (delta.getPrevious() == TasksAdapter.States.SELECT_MULTIPLE_TASKS) {
                            switchToDefaultState();
                        }
                    }
                });

        mIconBar.onShareTasks()
                .map(new Function<Object, List<Task>>() {
                    @Override
                    public List<Task> apply(@io.reactivex.annotations.NonNull Object o) throws Exception {
                        return mAdapter.getMultipleSelectedTasks();
                    }
                })
                .subscribe(new Consumer<List<Task>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull List<Task> tasks) throws Exception {
                        List<String> content = new ArrayList<>();
                        for (Task task : tasks) {
                            List<String> temp = new ArrayList<>();
                            if (!task.getTitle().isEmpty()) {
                                temp.add(task.getTitle());
                            }
                            if (!task.getDescription().isEmpty()) {
                                temp.add(task.getDescription());
                            }
                            content.add(TextUtils.join(" - ", temp));
                        }
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, TextUtils.join("\n", content));
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(sendIntent, MainActivity.this.getString(R.string.share_tasks)));
                    }
                });

        mIconBar.onStartAddTask().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        switchToAddTaskState();
                    }
                });

        mIconBar.newTaskData()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<TaskCreation>() {
                    @Override
                    public void accept(@NonNull TaskCreation taskCreation) throws Exception {
                        switchToSavingNewTaskState();
                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Function<TaskCreation, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(@NonNull TaskCreation taskCreation) throws Exception {
                        return ts
                                .addTask(taskCreation)
                                .observeOn(AndroidSchedulers.mainThread())
                                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends Integer>>() {
                                    @Override
                                    public ObservableSource<? extends Integer> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                        switchToAddTaskState();
                                        showServerError();
                                        return Observable.empty();
                                    }
                                });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Integer integer) throws Exception {
                        switchToDefaultState();
                    }
                })
                .subscribe(mUpdateTasksSubject);

        mIconBar.getCancelAddNewTaskClicks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        switchToDefaultState();
                    }
                });

        mUpdateTasksSubject
                .debounce(100, TimeUnit.MILLISECONDS)
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Object o) throws Exception {
                        updateScreenIn30SecondsIfItIsntUpdatedBefore();
                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Function<Object, Observable<List<Task>>>() {
                    @Override
                    public Observable<List<Task>> apply(@io.reactivex.annotations.NonNull Object o) throws Exception {
                        return ts
                                .getTasks()
                                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends List<Task>>>() {
                                    @Override
                                    public ObservableSource<? extends List<Task>> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                        return Observable.empty();
                                    }
                                });
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

    private void showServerError() {
        Snackbar
                .make(findViewById(R.id.tasks_coordinator_layout), R.string.error_in_server, getInteger(R.integer.error_message_length))
                .show();
    }

    private int getInteger(int resourceId) {
        return getResources().getInteger(resourceId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityIsDestroyed.onNext("");
    }

    private void updateScreenIn30SecondsIfItIsntUpdatedBefore() {
        /*
         1. if the user doesn't do any interaction in the interface, in 30 seconds the observable
            bellow will emit an item to mUpdateTasksSubject.
         1.a. this sill cause mUpdateTasksSubject to emit an item itself, which in turn will cancel
            the execution of this observable.
         2. if the user do any interaction, mUpdateTasksSubject will emit an item and the observable
            bellow will stop its execution.
         3. mUpdateTasksSubject will call this function again and restart the process

         Note that it's very hard to use takeUntil and doOnComplete to restart this method since
         when mUpdateTasksSubject is completed it will emit an item and the following will happen:
         1. this method will be called again
         2. since mUpdateTasksSubject is completed, takeUntil will emit an item
         3. the doOnComplete block will execute again
         4. this method will be called again. Go to step 1 above.

         instead of trying to be smart and workaround this I'll let mUpdateTasksSubject restart the
         process.
         */
        Observable.interval(30, TimeUnit.SECONDS)
                .takeUntil(mUpdateTasksSubject)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {
                        // ATTENTION: if I subscribe mUpdateTasksSubject directly, when the
                        // intervalObservable completes, the mUpdateTasksSubject will complete too,
                        // that's why I just call onNext in the subject.
                        mUpdateTasksSubject.onNext("");
                    }
                });
    }

    private void hideKeyboard() {
        Utils.hideKeyboard(this);
    }

    private void switchToDefaultState() {
        mState.setState(DEFAULT_STATE);
        hideKeyboard();
        mIconBar.switchToDefaultState(this);
    }

    private void switchToAddTaskState() {
        mState.setState(ADD_TASK_STATE);
        mIconBar.switchToAddTaskState();
    }

    private void switchToSavingNewTaskState() {
        mState.setState(SAVING_NEW_TASK_STATE);
        hideKeyboard();
        mIconBar.switchToSavingNewTaskState();
    }

    private void switchToSelectMultipleItemsState() {
        mState.setState(SELECT_MULTIPLE_ITEMS_STATE);
        mIconBar.switchToMultipleItemsSelectedState(this);
        hideKeyboard();
    }

    private void switchToState(int state) {
        switch (state) {
            case DEFAULT_STATE:
                switchToDefaultState();
                break;
            case ADD_TASK_STATE:
                switchToAddTaskState();
                break;
            case SAVING_NEW_TASK_STATE:
                switchToSavingNewTaskState();
                break;
            case SELECT_MULTIPLE_ITEMS_STATE:
                switchToSelectMultipleItemsState();
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
        Set<Integer> menuItemsToShow = mIconBar.getMenuItemsToShow();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setVisible(menuItemsToShow.contains(item.getItemId()));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = mIconBar.onOptionsItemSelected(item);
        return handled || super.onOptionsItemSelected(item);
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

    private static class IconBar {
        private final AddTaskView mAddTaskView;
        private final ShareTasksView mShareTasksView;

        static IconBar initializeToolbar(MainActivity activity) {
            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.tasks_toolbar);
            toolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.colorToolbarTitle));
            activity.setSupportActionBar(toolbar);
            return new IconBar(activity);
        }

        private IconBar(MainActivity activity) {
            this.mAddTaskView = new AddTaskView(activity);
            this.mShareTasksView = new ShareTasksView();
        }

        Observable<Object> onShareTasks() {
            return mShareTasksView.onShareTasks();
        }

        Observable<Object> onStartAddTask() {
            return mAddTaskView.onStartAddTask();
        }

        void switchToDefaultState(MainActivity activity) {
            mAddTaskView.switchToDefaultState();
            mShareTasksView.switchToHiddenState();
            activity.invalidateOptionsMenu();
        }

        void switchToMultipleItemsSelectedState(MainActivity activity) {
            mAddTaskView.switchToHiddenState();
            mShareTasksView.switchToVisibleState();
            activity.invalidateOptionsMenu();
        }

        void switchToAddTaskState() {
            mAddTaskView.switchToAddTaskState();
        }

        void switchToSavingNewTaskState() {
            mAddTaskView.switchToSavingNewTaskState();
        }

        Observable<TaskCreation> newTaskData() {
            return mAddTaskView.newTaskData();
        }

        Observable<Object> getCancelAddNewTaskClicks() {
            return mAddTaskView.getCancelAddNewTaskClicks();
        }

        boolean onOptionsItemSelected(MenuItem item) {
            // ATTENTION: WILL CAUSE SIDE EFFECTS!
            return mAddTaskView.onOptionsItemSelected(item)
                    || mShareTasksView.onOptionsItemSelected(item);
        }

        Set<Integer> getMenuItemsToShow() {
            Set<Integer> result = new HashSet<>();
            result.addAll(mAddTaskView.getMenuItemsToShow());
            result.addAll(mShareTasksView.getMenuItemsToShow());
            return result;
        }
    }

    private static class ShareTasksView {
        private List<Integer> menuItemsToShow = new ArrayList<>();
        private PublishSubject<Object> mShareTasksSubject = PublishSubject.create();

        void switchToHiddenState() {
            menuItemsToShow = Collections.emptyList();
        }

        void switchToVisibleState() {
            menuItemsToShow = Collections.singletonList(R.id.share_tasks);
        }

        List<Integer> getMenuItemsToShow() {
            return menuItemsToShow;
        }

        boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.share_tasks:
                    mShareTasksSubject.onNext("");
                    return true;
                default:
                    return false;
            }
        }

        Observable<Object> onShareTasks() {
            return mShareTasksSubject;
        }
    }

    private static class AddTaskView {
        private PublishSubject<Object> mAddTaskSubject = PublishSubject.create();
        private List<Integer> menuItemsToShow = Collections.singletonList(R.id.add_task);

        private final ProgressBar mProgressBar;
        private final EditText mTitleEditText;
        private final EditText mDescriptionEditText;
        private final ImageButton mAddSaveButton;
        private final ImageButton mAddCancelButton;
        private final List<View> allItems;
        private final Observable<Object> mSaveNewTaskClicks;
        private final Observable<Object> mCancelAddNewTaskClicks;

        AddTaskView(MainActivity activity) {
            mProgressBar = (ProgressBar) activity.findViewById(R.id.tasks_progress_bar);
            mTitleEditText = (EditText) activity.findViewById(R.id.tasks_add_task_title);
            mDescriptionEditText = (EditText) activity.findViewById(R.id.tasks_add_task_description);
            mAddSaveButton = (ImageButton) activity.findViewById(R.id.tasks_add_task_save);
            mAddCancelButton = (ImageButton) activity.findViewById(R.id.tasks_add_task_cancel);
            allItems = Arrays.asList(mProgressBar, mTitleEditText, mDescriptionEditText, mAddSaveButton, mAddCancelButton);

            mSaveNewTaskClicks = RxView.clicks(mAddSaveButton)
                    .takeUntil(activity.isDestroyedObservable());
            mCancelAddNewTaskClicks = RxView.clicks(mAddCancelButton)
                    .takeUntil(activity.isDestroyedObservable());
        }

        Observable<TaskCreation> newTaskData() {
            return mSaveNewTaskClicks.map(new Function<Object, TaskCreation>() {
                @Override
                public TaskCreation apply(@io.reactivex.annotations.NonNull Object o) throws Exception {
                    return getNewTaskData();
                }
            });
        }

        Observable<Object> getCancelAddNewTaskClicks() {
            return mCancelAddNewTaskClicks;
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


        private void hideAllFields() {
            for (View view : allItems) {
                view.setVisibility(View.GONE);
            }
        }

        void switchToDefaultState() {
            menuItemsToShow = Collections.singletonList(R.id.add_task);
            cleanFields();
            hideAllFields();
        }

        void switchToAddTaskState() {
            showOnly(mTitleEditText, mDescriptionEditText, mAddSaveButton, mAddCancelButton);
        }

        void switchToSavingNewTaskState() {
            showOnly(mProgressBar);
        }

        private void cleanFields() {
            mTitleEditText.setText("");
            mDescriptionEditText.setText("");
        }

        TaskCreation getNewTaskData() {
            TaskCreation taskData = new TaskCreation();
            taskData.setTitle(mTitleEditText.getText().toString());
            taskData.setDescription(mDescriptionEditText.getText().toString());
            return taskData;
        }

        void switchToHiddenState() {
            cleanFields();
            hideAllFields();
            menuItemsToShow = Collections.emptyList();
        }

        Collection<? extends Integer> getMenuItemsToShow() {
            return menuItemsToShow;
        }

        boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.add_task:
                    mAddTaskSubject.onNext("");
                    return true;
                default:
                    return false;
            }
        }

        Observable<Object> onStartAddTask() {
            return mAddTaskSubject;
        }
    }

    private ObservableSource<Object> isDestroyedObservable() {
        return mActivityIsDestroyed;
    }

    private static final int DEFAULT_STATE = 0;
    private static final int ADD_TASK_STATE = 1;
    private static final int SAVING_NEW_TASK_STATE = 2;
    private static final int SELECT_MULTIPLE_ITEMS_STATE = 3;

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
