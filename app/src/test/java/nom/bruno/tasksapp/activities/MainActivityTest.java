package nom.bruno.tasksapp.activities;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import nom.bruno.tasksapp.BuildConfig;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.Singletons;
import nom.bruno.tasksapp.models.TaskCreation;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.services.TaskServiceStub;
import nom.bruno.tasksapp.shadows.ShadowToolbarActionBar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowToolbarActionBar.class})
public class MainActivityTest {
    private static TestScheduler myScheduler = new TestScheduler();

    @Before
    public void setup() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(@NonNull Scheduler scheduler) throws Exception {
                return myScheduler;
            }
        });
        Singletons.setTaskService(new TaskServiceStub());
    }

    @After
    public void after() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void testIconsOnInitialState() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        myScheduler.triggerActions();
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu.hasVisibleItems());
        assertDefaultIcons(activity, true);
        assertShareIcons(activity, false);
    }

    @Test
    public void testIconsOnShareState() {
        // make sure there will be an item in the list
        TaskService ts = Singletons.getTaskService(null);
        TaskCreation tc = new TaskCreation();
        tc.setTitle("t");
        tc.setDescription("d");
        ts.addTask(tc).blockingFirst();
        // get first item
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        myScheduler.triggerActions();
        RecyclerView rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        assertNotNull(rvTasks);
        View firstItem = rvTasks.getChildAt(0);
        assertNotNull(firstItem);
        // perform long click, this should change the app's state
        firstItem.performLongClick();
        myScheduler.triggerActions();
        // check the icons
        Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu.hasVisibleItems());
        assertShareIcons(activity, true);
        assertDefaultIcons(activity, false);
    }

    @SuppressLint("SetTextI18n")
    @Test
    public void testItemBeingEditedMustNotBeUpdated() {
        // make sure there will be an item in the list
        TaskService ts = Singletons.getTaskService(null);
        TaskCreation tc = new TaskCreation();
        tc.setTitle("t");
        tc.setDescription("d");
        ts.addTask(tc).blockingFirst();

        // get first item
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        myScheduler.triggerActions();
        RecyclerView rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        assertNotNull(rvTasks);
        View firstItem = rvTasks.getChildAt(0);
        assertNotNull(firstItem);

        // select first item
        firstItem.performClick();
        myScheduler.triggerActions();

        // starts editing the item
        View editButton = firstItem.findViewById(R.id.item_task_edit);
        editButton.performClick();
        myScheduler.triggerActions();
        assertEquals(firstItem.findViewById(R.id.item_task_edit).getVisibility(), View.GONE);
        assertEquals(firstItem.findViewById(R.id.item_task_delete).getVisibility(), View.GONE);
        assertEquals(firstItem.findViewById(R.id.item_task_title).getVisibility(), View.GONE);
        assertEquals(firstItem.findViewById(R.id.item_task_description).getVisibility(), View.GONE);
        assertEquals(firstItem.findViewById(R.id.item_task_edit_title).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_edit_description).getVisibility(), View.VISIBLE);

        // change title and description
        ((EditText) firstItem.findViewById(R.id.item_task_edit_title)).setText("New title");
        myScheduler.triggerActions();
        ((EditText) firstItem.findViewById(R.id.item_task_edit_description)).setText("New description");
        myScheduler.triggerActions();

        // force a screen update (it could have happened because 30 seconds have passed)
        activity.forceTasksUpdate();
        myScheduler.advanceTimeBy(101, TimeUnit.MILLISECONDS);
        myScheduler.triggerActions();

        // title and description must contain the new values being edited
        rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        firstItem = rvTasks.getChildAt(0);

        assertEquals(((EditText) firstItem.findViewById(R.id.item_task_edit_title)).getText().toString(), "New title");
        assertEquals(((EditText) firstItem.findViewById(R.id.item_task_edit_description)).getText().toString(), "New description");
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

    @BeforeClass
    public static void setupClass() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(@NonNull Callable<Scheduler> schedulerCallable) throws Exception {
                return myScheduler;
            }
        });
        RxJavaPlugins.setIoSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(@NonNull Scheduler scheduler) throws Exception {
                return Schedulers.trampoline();
            }
        });
        RxJavaPlugins.setComputationSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(@NonNull Scheduler scheduler) throws Exception {
                return myScheduler;
            }
        });
    }
}

