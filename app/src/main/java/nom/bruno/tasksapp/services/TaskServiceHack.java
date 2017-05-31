package nom.bruno.tasksapp.services;

import android.content.Context;

import nom.bruno.tasksapp.Constants;
import nom.bruno.tasksapp.services.storage.TasksStorage;

public class TaskServiceHack {
    private static TaskService myInstance;

    public static void setInstance(TaskService ts) {
        TaskServiceHack.myInstance = ts;
    }

    public static TaskService getInstance(Context context) {
        if (myInstance == null) {
            myInstance = new TaskServiceImpl(TasksStorage.create(context));
        }
        return myInstance;
    }
}
