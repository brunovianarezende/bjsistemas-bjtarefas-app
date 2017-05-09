package nom.bruno.tasksapp.services;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

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
    private static TaskApi mInternalApi = createTaskApi(Constants.INTERNAL_SERVICE_URL);
    private static TaskApi mExternalApi = createTaskApi(Constants.EXTERNAL_SERVICE_URL);
    private List<TaskApi> mCandidates = Arrays.asList(mExternalApi, mInternalApi);

    private static TaskApi createTaskApi(String url) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();
        return retrofit.create(TaskApi.class);
    }

    private <T> Observable<T> executeInCorrectApi(final Function<TaskApi, Observable<T>> bla) {
        return Observable
                .fromIterable(mCandidates)
                .concatMap(new Function<TaskApi, Observable<T>>() {
                    @Override
                    public Observable<T> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
                        return bla.apply(taskApi).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends T>>() {
                            @Override
                            public ObservableSource<? extends T> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                return Observable.empty();
                            }
                        });
                    }
                })
                .firstElement()
                .toObservable();
    }

    public Observable<List<Task>> getTasks() {
        return executeInCorrectApi(new Function<TaskApi, Observable<Result<List<Task>>>>() {
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

//        return Observable
//                .fromIterable(mCandidates)
//                .concatMap(new Function<TaskApi, Observable<Result<List<Task>>>>() {
//                    @Override
//                    public Observable<Result<List<Task>>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
//                        return taskApi.getTasks().onErrorResumeNext(new Function<Throwable, ObservableSource<? extends Result<List<Task>>>>() {
//                            @Override
//                            public ObservableSource<? extends Result<List<Task>>> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
//                                return Observable.empty();
//                            }
//                        });
//                    }
//                })
//                .first(new Result<List<Task>>())
//                .map(new Function<Result<List<Task>>, List<Task>>() {
//                    @Override
//                    public List<Task> apply(@io.reactivex.annotations.NonNull Result<List<Task>> listResult) throws Exception {
//                        return listResult.getData();
//                    }
//                }).toObservable();

//        return mExternalApi.getTasks()
//                .onErrorResumeNext(mInternalApi.getTasks())
//                .map(new Function<Result<List<Task>>, List<Task>>() {
//
//                    @Override
//                    public List<Task> apply(@NonNull Result<List<Task>> listResult) throws Exception {
//                        return listResult.getData();
//                    }
//                });
    }

    public Observable<MyVoid> deleteTask(final int id) {
        return executeInCorrectApi(new Function<TaskApi, Observable<Result<Void>>>() {
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
//        return Observable
//                .fromIterable(mCandidates)
//                .concatMap(new Function<TaskApi, Observable<Result<Void>>>() {
//                    @Override
//                    public Observable<Result<Void>> apply(@io.reactivex.annotations.NonNull TaskApi taskApi) throws Exception {
//                        return taskApi.deleteTask(id).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends Result<Void>>>() {
//                            @Override
//                            public ObservableSource<? extends Result<Void>> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
//                                return Observable.empty();
//                            }
//                        });
//                    }
//                })
//                .first(new Result<Void>())
//                .map(new Function<Result<Void>, MyVoid>() {
//                    @Override
//                    public MyVoid apply(@io.reactivex.annotations.NonNull Result<Void> voidResult) throws Exception {
//                        return MyVoid.INSTANCE;
//                    }
//                }).toObservable();

//        return mExternalApi.deleteTask(id)
//                .onErrorResumeNext(mInternalApi.deleteTask(id))
//                .map(new Function<Result<Void>, MyVoid>() {
//                    @Override
//                    public MyVoid apply(@NonNull Result<Void> voidResult) throws Exception {
//                        return MyVoid.INSTANCE;
//                    }
//                });
    }

    public Observable<MyVoid> updateTask(int id, TaskUpdate taskUpdate) {
        return mExternalApi.updateTask(id, taskUpdate)
                .onErrorResumeNext(mInternalApi.updateTask(id, taskUpdate))
                .map(new Function<Result<Void>, MyVoid>() {
                    @Override
                    public MyVoid apply(@NonNull Result<Void> voidResult) throws Exception {
                        return MyVoid.INSTANCE;
                    }
                });
    }

    public Observable<Integer> addTask(TaskCreation taskCreation) {
        return mExternalApi.addTask(taskCreation)
                .onErrorResumeNext(mInternalApi.addTask(taskCreation))
                .map(new Function<Result<Integer>, Integer>() {
                    @Override
                    public Integer apply(@NonNull Result<Integer> integerResult) throws Exception {
                        return integerResult.getData();
                    }
                });
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
}
