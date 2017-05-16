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

public class TaskService {
    private static TaskService myInstance;
    private final TasksStorage mTasksStorage;

    private TaskService(TasksStorage tasksStorage) {
        mTasksStorage = tasksStorage;
    }

    public static TaskService getInstance(Context context) {
        if (myInstance == null) {
            myInstance = new TaskService(TasksStorage.create(context));
        }
        return myInstance;
    }

    private TaskApiExecutor executor = new TaskApiExecutor();

    public Observable<List<Task>> getTasks() {
        return executor.call(new Function<TaskApi, Observable<Result<List<Task>>>>() {
            @Override
            public Observable<Result<List<Task>>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
                return taskApi.getTasks();
            }
        }).map(new Function<Result<List<Task>>, List<Task>>() {
            @Override
            public List<Task> apply(@io.reactivex.annotations.NonNull Result<List<Task>> listResult) throws Exception {
                List<Task> tasks = listResult.getData();
                mTasksStorage.setTasks(tasks);
                return tasks;
            }
        });
    }

    private Observable<List<Task>> getTasksWithoutSideEffects() {
        return executor.call(new Function<TaskApi, Observable<Result<List<Task>>>>() {
            @Override
            public Observable<Result<List<Task>>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
                return taskApi.getTasks();
            }
        }).map(new Function<Result<List<Task>>, List<Task>>() {
            @Override
            public List<Task> apply(@io.reactivex.annotations.NonNull Result<List<Task>> listResult) throws Exception {
                return listResult.getData();
            }
        });
    }

    public Observable<MyVoid> deleteTask(final int id) {
        return executor.call(new Function<TaskApi, Observable<Result<Void>>>() {
            @Override
            public Observable<Result<Void>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
                return taskApi.deleteTask(id);
            }
        }).map(new Function<Result<Void>, MyVoid>() {
            @Override
            public MyVoid apply(@io.reactivex.annotations.NonNull Result<Void> voidResult) throws Exception {
                return MyVoid.INSTANCE;
            }
        });
    }

    public Observable<MyVoid> updateTask(final int id, final TaskUpdate taskUpdate) {
        return executor.call(new Function<TaskApi, Observable<Result<Void>>>() {
            @Override
            public Observable<Result<Void>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
                return taskApi.updateTask(id, taskUpdate);
            }
        }).map(new Function<Result<Void>, MyVoid>() {
            @Override
            public MyVoid apply(@io.reactivex.annotations.NonNull Result<Void> voidResult) throws Exception {
                return MyVoid.INSTANCE;
            }
        });
    }

    public Observable<Integer> addTask(final TaskCreation taskCreation) {
        return executor.call(new Function<TaskApi, Observable<Result<Integer>>>() {
            @Override
            public Observable<Result<Integer>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
                return taskApi.addTask(taskCreation);
            }
        }).map(new Function<Result<Integer>, Integer>() {
            @Override
            public Integer apply(@NonNull Result<Integer> integerResult) throws Exception {
                return integerResult.getData();
            }
        });
    }

    public Observable<TasksDelta> getTasksDelta() {
        return getTasksWithoutSideEffects()
                .map(new Function<List<Task>, TasksDelta>() {
                    @Override
                    public TasksDelta apply(@io.reactivex.annotations.NonNull List<Task> tasks) throws Exception {
                        TasksDelta result = new TasksDelta();
                        Map<Integer, Task> oldTasksMapping = new HashMap<>();
                        for (Task task : getPersistedTasks()) {
                            oldTasksMapping.put(task.getId(), task);
                        }
                        Map<Integer, Task> newTasksMapping = new HashMap<>();
                        for (Task task : tasks) {
                            newTasksMapping.put(task.getId(), task);
                            if (!oldTasksMapping.containsKey(task.getId())) {
                                result.getNewTasks().add(task);
                            } else {
                                Task oldTask = oldTasksMapping.get(task.getId());
                                if (!oldTask.getDescription().equals(task.getDescription())
                                        || !oldTask.getTitle().equals(task.getTitle())) {
                                    result.getUpdatedTasks().add(task);
                                }
                            }
                        }
                        Set<Integer> deletedIds = new HashSet<>(oldTasksMapping.keySet());
                        deletedIds.removeAll(newTasksMapping.keySet());
                        result.getDeletedTasksIds().addAll(deletedIds);
                        mTasksStorage.setTasks(tasks);
                        return result;
                    }
                });
    }

    public List<Task> getPersistedTasks() {
        return mTasksStorage.getTasks();
    }

    public interface TaskApi {
        @GET("tasks")
        Observable<Result<List<Task>>> getTasks();

        @POST("tasks")
        Observable<Result<Integer>> addTask(@Body TaskCreation taskCreation);

        @DELETE("tasks/{id}")
        Observable<Result<Void>> deleteTask(@Path("id") int id);

        @PUT("tasks/{id}")
        Observable<Result<Void>> updateTask(@Path("id") int id, @Body TaskUpdate taskUpdate);

    }

    private static class TaskApiWrapper {
        final TaskApi taskApi;
        final String url;

        public TaskApiWrapper(TaskApi taskApi, String url) {
            this.taskApi = taskApi;
            this.url = url;
        }
    }

    private static class TaskApiExecutor {
        private TaskApiWrapper mInternalApi = createTaskApi(Constants.INTERNAL_SERVICE_URL);
        private TaskApiWrapper mExternalApi = createTaskApi(Constants.EXTERNAL_SERVICE_URL);
        private List<TaskApiWrapper> mCandidates = Arrays.asList(mExternalApi, mInternalApi);

        private TaskApiWrapper createTaskApi(String url) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .build();
            return new TaskApiWrapper(retrofit.create(TaskApi.class), url);
        }

        public <T> Observable<T> call(final Function<TaskApi, Observable<T>> callable) {
            return Observable
                    .fromIterable(mCandidates)
                    .concatMap(new Function<TaskApiWrapper, Observable<T>>() {
                        @Override
                        public Observable<T> apply(@io.reactivex.annotations.NonNull final TaskApiWrapper taskApiWrapper) throws Exception {
                            return callable.apply(taskApiWrapper.taskApi).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                    List<TaskApiWrapper> candidates = Arrays.asList(mExternalApi, mInternalApi);
                                    Collections.sort(candidates, new Comparator<TaskApiWrapper>() {
                                        @Override
                                        public int compare(TaskApiWrapper o1, TaskApiWrapper o2) {
                                            if (o1.url.equals(taskApiWrapper.url)) {
                                                return 1;
                                            } else if (o2.url.equals(taskApiWrapper.url)) {
                                                return -1;
                                            } else {
                                                return 0;
                                            }
                                        }
                                    });
                                    mCandidates = candidates;
                                    return Observable.empty();
                                }
                            });
                        }
                    })
                    .firstElement()
                    .toObservable();
        }

    }
}
