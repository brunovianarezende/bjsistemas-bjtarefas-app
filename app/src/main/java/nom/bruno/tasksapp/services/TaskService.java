package nom.bruno.tasksapp.services;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import nom.bruno.tasksapp.Constants;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Result;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.models.TaskUpdate;
import nom.bruno.tasksapp.models.TasksDelta;
import nom.bruno.tasksapp.services.storage.TasksStorage;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TaskService {
    Observable<List<Task>> getTasks();
    Observable<MyVoid> deleteTask(final int id);
    Observable<MyVoid> updateTask(final int id, final TaskUpdate taskUpdate);
    Observable<Integer> addTask(final TaskCreation taskCreation);
    Observable<TasksDelta> getTasksDelta();
    List<Task> getPersistedTasks();
}

