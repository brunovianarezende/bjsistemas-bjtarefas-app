package nom.bruno.tasksapp.services;

import java.util.List;

import io.reactivex.Observable;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.models.TaskUpdate;
import nom.bruno.tasksapp.models.TasksDelta;

public interface TaskService {
    Observable<List<Task>> getTasks();

    Observable<MyVoid> deleteTask(final int id);

    Observable<MyVoid> updateTask(final int id, final TaskUpdate taskUpdate);

    Observable<MyVoid> moveTask(final int id, final int position);

    Observable<Integer> addTask(final TaskCreation taskCreation);

    Observable<TasksDelta> getTasksDelta();

    List<Task> getPersistedTasks();
}

