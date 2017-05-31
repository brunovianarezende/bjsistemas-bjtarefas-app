package nom.bruno.tasksapp.activities;

import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import nom.bruno.tasksapp.BuildConfig;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.services.TaskServiceHack;
import nom.bruno.tasksapp.services.TaskServiceStub;
import nom.bruno.tasksapp.shadows.ShadowToolbarActionBar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowToolbarActionBar.class})
public class MainActivityTest {
    @Before
    public void setup() {
        TaskServiceHack.setInstance(new TaskServiceStub());
    }

    @Test
    public void testIconsOnInitialState() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu.hasVisibleItems());
        assertDefaultIcons(activity, true);
        assertShareIcons(activity, false);
    }

    @Test
    public void testIconsOnShareState() {
        // make sure there will be an item in the list
        TaskService ts = TaskServiceHack.getInstance(null);
        TaskCreation tc = new TaskCreation();
        tc.setTitle("t");
        tc.setDescription("d");
        ts.addTask(tc).blockingFirst();
        // get first item
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        RecyclerView rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        assertNotNull(rvTasks);
        View firstItem = rvTasks.getChildAt(0);
        assertNotNull(firstItem);
        // perform long click, this should change the app's state
        firstItem.performLongClick();
        // check the icons
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu.hasVisibleItems());
        assertShareIcons(activity, true);
        assertDefaultIcons(activity, false);
    }


    private void assertShareIcons(MainActivity activity, boolean visible) {
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertIcon(menu, visible, R.id.share_tasks);
        ShadowToolbarActionBar actionBar = Shadow.extract(activity.getSupportActionBar());
        assertEquals(actionBar.getDisplayHomeAsUpEnabled(), visible);
    }

    private void assertDefaultIcons(MainActivity activity, boolean visible) {
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertIcon(menu, visible, R.id.add_task);
    }

    private void assertIcon(Menu menu, boolean visible, int iconId) {
        if (visible) {
            assertTrue(menu.findItem(iconId).isVisible());
        } else {
            assertTrue(menu.findItem(iconId) == null || !menu.findItem(iconId).isVisible());
        }
    }
}
