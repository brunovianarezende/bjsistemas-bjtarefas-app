package nom.bruno.tasksapp.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.models.TaskUpdate;
import nom.bruno.tasksapp.models.TasksDelta;

public class TaskServiceStub implements TaskService {
    private List<Task> mTasks = new ArrayList<>();
    private int mCurrentId = 1;

    private void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    private List<Task> cloneTasks(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            Task newTask = new Task();
            newTask.setId(task.getId());
            newTask.setDescription(task.getDescription());
            newTask.setTitle(task.getTitle());
            result.add(newTask);
        }
        return result;
    }

    @Override
    public Observable<List<Task>> getTasks() {
        sleep();
        return Observable.just(cloneTasks(mTasks));
    }

    @Override
    public Observable<MyVoid> deleteTask(int id) {
        sleep();
        List<Task> newTasks = new ArrayList<>();
        for (Task task : mTasks) {
            if (task.getId() != id) {
                newTasks.add(task);
            }
        }
        mTasks.clear();
        mTasks.addAll(newTasks);
        return Observable.just(MyVoid.INSTANCE);
    }

    @Override
    public Observable<MyVoid> updateTask(int id, TaskUpdate taskUpdate) {
        sleep();
        for (Task task : mTasks) {
            if (task.getId() == id) {
                task.setTitle(taskUpdate.getTitle());
                task.setDescription(taskUpdate.getDescription());
            }
        }
        return Observable.just(MyVoid.INSTANCE);
    }

    @Override
    public Observable<Integer> addTask(TaskCreation taskCreation) {
        Task task = new Task();
        sleep();
        task.setId(mCurrentId);
        mCurrentId += 1;
        task.setTitle(taskCreation.getTitle());
        task.setDescription(taskCreation.getDescription());
        mTasks.add(task);
        return Observable.just(task.getId());
    }

    @Override
    public Observable<TasksDelta> getTasksDelta() {
        sleep();
        return Observable.just(new TasksDelta());
    }

    @Override
    public List<Task> getPersistedTasks() {
        return Collections.emptyList();
    }
}
