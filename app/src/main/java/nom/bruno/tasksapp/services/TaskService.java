package nom.bruno.tasksapp.services;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import nom.bruno.tasksapp.Constants;
import nom.bruno.tasksapp.models.Result;
import nom.bruno.tasksapp.models.Task;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

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

    public interface TaskApi {
        @GET("tasks")
        Observable<Result<List<Task>>> getTasks();
    }
}
