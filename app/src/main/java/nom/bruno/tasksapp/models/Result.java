package nom.bruno.tasksapp.models;

public class Result<T> {
    private boolean success;
    private T data;

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }
}
