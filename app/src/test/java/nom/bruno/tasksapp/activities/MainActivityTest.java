package nom.bruno.tasksapp.activities;

import android.view.Menu;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import nom.bruno.tasksapp.BuildConfig;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.services.TaskServiceHack;
import nom.bruno.tasksapp.services.TaskServiceStub;

import static junit.framework.Assert.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MainActivityTest {
    @Before
    public void setup() {
        TaskServiceHack.setInstance(new TaskServiceStub());
    }

    @Test
    public void testInitialIcons() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu.hasVisibleItems());
        assertDefaultIcons(menu, true);
        assertShareIcons(menu, false);
    }

    private void assertShareIcons(Menu menu, boolean visible) {
        assertIcon(menu, visible, R.id.share_tasks);
        assertIcon(menu, visible, android.R.id.home);
    }

    private void assertDefaultIcons(Menu menu, boolean visible) {
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
