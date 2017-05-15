package nom.bruno.tasksapp.services.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import nom.bruno.tasksapp.models.Task;

public class TasksStorage {
    private static final String TASKS_STORAGE_FILENAME = "persistent_tasks";
    private static final String TASKS_KEY = "persisted_tasks";

    private final SharedPreferences mSharedPreferences;

    public TasksStorage(SharedPreferences sharedPreferences) {
        this.mSharedPreferences = sharedPreferences;
    }

    public List<Task> getTasks() {
        if (mSharedPreferences.contains(TASKS_KEY)) {
            String unserialised = mSharedPreferences.getString(TASKS_KEY, "");
            Gson gson = new Gson();
            try {
                Type listType = new TypeToken<List<Task>>() {
                }.getType();
                return gson.fromJson(unserialised, listType);

            } catch (JsonSyntaxException e) {
                mSharedPreferences.edit().remove(TASKS_KEY).commit();
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    public void setTasks(List<Task> tasks) {
        Gson gson = new Gson();
        mSharedPreferences.edit().putString(TASKS_KEY, gson.toJson(tasks)).commit();
    }

    public static TasksStorage create(Context context) {
        return new TasksStorage(context.getSharedPreferences(TASKS_STORAGE_FILENAME, Context.MODE_PRIVATE));
    }
}