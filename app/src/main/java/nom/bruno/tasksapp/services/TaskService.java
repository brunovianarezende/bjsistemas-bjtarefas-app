package nom.bruno.tasksapp.services;

import android.support.annotation.NonNull;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import nom.bruno.tasksapp.Constants;
import nom.bruno.tasksapp.models.MyVoid;
import nom.bruno.tasksapp.models.Result;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TaskUpdate;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class TaskService {
    private static TaskApi mInternalApi = createTaskApi(Constants.INTERNAL_SERVICE_URL);
    private static TaskApi mExternalApi = createTaskApi(Constants.EXTERNAL_SERVICE_URL);

    private static TaskApi createTaskApi(String url) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();
        return retrofit.create(TaskApi.class);
    }

    public Observable<List<Task>> getTasks() {
        return mExternalApi.getTasks()
                .onErrorResumeNext(mInternalApi.getTasks())
                .map(new Function<Result<List<Task>>, List<Task>>() {

                    @Override
                    public List<Task> apply(@NonNull Result<List<Task>> listResult) throws Exception {
                        return listResult.getData();
                    }
                });
    }

    public Observable<MyVoid> deleteTask(int id) {
        return mExternalApi.deleteTask(id)
                .onErrorResumeNext(mInternalApi.deleteTask(id))
                .map(new Function<Result<Void>, MyVoid>() {
                    @Override
                    public MyVoid apply(@NonNull Result<Void> voidResult) throws Exception {
                        return MyVoid.INSTANCE;
                    }
                });
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

    public interface TaskApi {
        @GET("tasks")
        Observable<Result<List<Task>>> getTasks();

        @DELETE("tasks/{id}")
        Observable<Result<Void>> deleteTask(@Path("id") int id);

        @PUT("tasks/{id}")
        Observable<Result<Void>> updateTask(@Path("id") int id, @Body TaskUpdate taskUpdate);
    }
}
