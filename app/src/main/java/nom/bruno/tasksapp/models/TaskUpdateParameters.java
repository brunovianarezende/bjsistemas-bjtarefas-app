package nom.bruno.tasksapp.models;

public class TaskUpdateParameters {
    private int taskId;
    private TaskUpdate updateData = new TaskUpdate();

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public TaskUpdate getUpdateData() {
        return updateData;
    }

    public void setUpdateData(TaskUpdate updateData) {
        this.updateData = updateData;
    }
}
