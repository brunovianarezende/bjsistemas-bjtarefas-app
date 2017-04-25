package nom.bruno.tasksapp.services;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import nom.bruno.tasksapp.models.Result;
import nom.bruno.tasksapp.models.Task;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class TaskService {
    public static final String BASE_URL = "http://10.0.2.2:8000/";
    private TaskApi mTaskApi;

    public TaskService() {
        RxJava2CallAdapterFactory rxAdapter = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(rxAdapter)
                .build();
        this.mTaskApi = retrofit.create(TaskApi.class);
    }

    public TaskApi getTaskApi() {
        return mTaskApi;
    }

    public interface TaskApi {
        @GET("tasks")
        Observable<Result<List<Task>>> getTasks();
    }
}
