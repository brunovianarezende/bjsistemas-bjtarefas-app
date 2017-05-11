package nom.bruno.tasksapp.models;

import java.util.ArrayList;
import java.util.List;

public class TasksDelta {
    private List<Task> newTasks = new ArrayList<>();
    private List<Task> updatedTasks = new ArrayList<>();
    private List<Integer> deletedTasksIds = new ArrayList<>();

    public List<Task> getNewTasks() {
        return newTasks;
    }

    public void setNewTasks(List<Task> newTasks) {
        this.newTasks = newTasks;
    }

    public List<Task> getUpdatedTasks() {
        return updatedTasks;
    }

    public void setUpdatedTasks(List<Task> updatedTasks) {
        this.updatedTasks = updatedTasks;
    }

    public List<Integer> getDeletedTasksIds() {
        return deletedTasksIds;
    }

    public void setDeletedTasksIds(List<Integer> deletedTasksIds) {
        this.deletedTasksIds = deletedTasksIds;
    }

    public boolean isEmpty() {
        return getTotalNumberOfChanges() == 0;
    }

    public int getTotalNumberOfChanges() {
        return newTasks.size() + updatedTasks.size() + deletedTasksIds.size();
    }
}
