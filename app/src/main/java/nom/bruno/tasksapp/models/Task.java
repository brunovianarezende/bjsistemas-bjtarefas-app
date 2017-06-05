package nom.bruno.tasksapp.models;

import android.text.TextUtils;

public class Task {
    private int id;
    private String title;
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Task other) {
        return this.getId() == other.getId();
    }

    public boolean isContentEquals(Task other) {
        if (other == null) {
            return false;
        }
        return TextUtils.equals(title, other.getTitle())
                && TextUtils.equals(description, other.getDescription());
    }
}
