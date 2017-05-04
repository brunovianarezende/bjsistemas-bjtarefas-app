package nom.bruno.tasksapp.models;

import android.support.annotation.NonNull;

public class TaskCreation {
    private String title = "";
    private String description = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }
}
