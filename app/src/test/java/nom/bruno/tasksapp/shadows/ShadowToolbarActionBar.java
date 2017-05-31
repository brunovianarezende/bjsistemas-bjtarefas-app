package nom.bruno.tasksapp.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(className = "android.support.v7.app.ToolbarActionBar")
public class ShadowToolbarActionBar {

    private boolean mDisplayHomeAsUp = false;

    public boolean getDisplayHomeAsUpEnabled() {
        return this.mDisplayHomeAsUp;
    }

    @Implementation
    public void setDisplayHomeAsUpEnabled(boolean displayHomeAsUp) {
        this.mDisplayHomeAsUp = displayHomeAsUp;
    }
}
