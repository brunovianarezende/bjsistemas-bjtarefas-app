package nom.bruno.tasksapp.view.adapters;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public abstract class BaseItemTouchHelperCallback extends ItemTouchHelper.Callback {
    /* this is the same value used by default in ItemTouchHelper.Callback. I'm allowing it to be
     * changed for tests in Robolectric, usually 0.1f is enough in tests. If this value isn't
     * changed, the default implementation of drag detection won't work in tests.
     *
     * I tried using Shadows to override the getMoveThreshold method in ItemTouchHelper.Callback,
     * but I didn't succeed.
     */
    public static float moveThreshold = 0.5f;

    @Override
    public float getMoveThreshold(RecyclerView.ViewHolder viewHolder) {
        return BaseItemTouchHelperCallback.moveThreshold;
    }
}
