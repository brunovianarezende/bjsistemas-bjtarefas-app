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
            if(!Constants.USE_STUBS) {
                myInstance = new TaskServiceImpl(TasksStorage.create(context));
            }
            else {
                myInstance = new TaskServiceStub();
            }
        }
        return myInstance;
    }


}
