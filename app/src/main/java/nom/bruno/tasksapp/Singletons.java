package nom.bruno.tasksapp;

import android.content.Context;

import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.services.TaskServiceImpl;
import nom.bruno.tasksapp.services.storage.TasksStorage;

// I will avoid using Dagger as much as needed. To make tests easier, I'll create this class
// where singletons might be retrieved or instantiated. It is not pretty, but IMHO is less ugly
// than using Dagger.
public class Singletons {
    private static TaskService mTaskService;

    public static void setTaskService(TaskService taskService) {
        Singletons.mTaskService = taskService;
    }

    public static TaskService getTaskService(Context context) {
        if (mTaskService == null) {
            mTaskService = new TaskServiceImpl(TasksStorage.create(context));
        }
        return mTaskService;
    }
}
