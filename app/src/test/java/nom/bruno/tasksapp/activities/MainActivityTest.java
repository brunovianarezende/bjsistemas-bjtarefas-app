package nom.bruno.tasksapp.activities;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
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

    @SuppressLint("SetTextI18n")
    @Test
    public void testItemBeingEditedMustNotBeMangledDueToViewReuse() {
        /*
         * Depending on how many items there are in the recycler view, if the item appears in the
         * screen and other details, the behaviour of view reuse might change. The reuse and
         * biding might start from first item and go up to the last item or start at the end
         * and go down to the first item. This means that an object might be rebound to a view
         * different from the one it was bound before, which might cause problems if the
         * implementation expects otherwise. Robolectric behaves slightly different from the
         * emulator and real devices: for a small number of items, if notifyDataSetChanged() is
         * called, the objects will be rebounded to the same view. However, for large number of
         * items, if you focus on the last item, the behaviour is the same, the objects won't be
         * bounded for the same view if notifyDataSetChanged() is called. One might object that
         * notifyDataSetChanged() must not be called, but this is an implementation detail and
         * we must be sure that we won't have regressions if we use it.
         *
         * My theory is that when we use a large number of items, Robolectric will consider that
         * there will be items that won't be shown in the screen and since we focus on the last
         * item, the rebound algorithm will proceed from end to start. Actually, I read the code for
         * some time and I'm somewhat confident that the behaviour will be something like what
         * I described.
         */
        // make sure there will be a large number of items in the list
        TaskService ts = Singletons.getTaskService(null);
        TaskCreation tc = new TaskCreation();
        int numItems = 10;
        for (int i = 0; i < numItems; i++) {
            tc.setTitle("t");
            tc.setDescription("d");
            ts.addTask(tc).blockingFirst();
        }

        // last item position
        int itemPosition = numItems - 1;

        // get item
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        myScheduler.triggerActions();
        RecyclerView rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        assertNotNull(rvTasks);
        View item = rvTasks.getChildAt(itemPosition);
        assertNotNull(item);

        // select item
        item.performClick();
        myScheduler.triggerActions();

        // starts editing it
        View editButton = item.findViewById(R.id.item_task_edit);
        editButton.performClick();
        myScheduler.triggerActions();
        assertEquals(item.findViewById(R.id.item_task_edit).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_delete).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_title).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_description).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_edit_title).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_edit_description).getVisibility(), View.VISIBLE);

        // change title and description
        ((EditText) item.findViewById(R.id.item_task_edit_title)).setText("New title");
        myScheduler.triggerActions();
        ((EditText) item.findViewById(R.id.item_task_edit_description)).setText("New description");
        myScheduler.triggerActions();

        // force a screen update (it could have happened because 30 seconds have passed)
        activity.forceTasksUpdate();
        myScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        myScheduler.triggerActions();

        // title and description must contain the new values being edited
        rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        item = rvTasks.getChildAt(itemPosition);

        assertEquals(((EditText) item.findViewById(R.id.item_task_edit_title)).getText().toString(), "New title");
        assertEquals(((EditText) item.findViewById(R.id.item_task_edit_description)).getText().toString(), "New description");
    }

    @Test
    public void testBugWhenAnItemIsEditedEditionCancelledAndTasksAreUpdated() {
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

        // cancel edition
        firstItem.findViewById(R.id.item_task_cancel_edit).performClick();
        myScheduler.triggerActions();
        assertEquals(firstItem.findViewById(R.id.item_task_edit).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_delete).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_title).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_description).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_edit_title).getVisibility(), View.GONE);
        assertEquals(firstItem.findViewById(R.id.item_task_edit_description).getVisibility(), View.GONE);

        // force a screen update (it could have happened because 30 seconds have passed)
        activity.forceTasksUpdate();
        myScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        myScheduler.triggerActions();

        // the state of the item must not have changed
        assertEquals(firstItem.findViewById(R.id.item_task_edit).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_delete).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_title).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_description).getVisibility(), View.VISIBLE);
        assertEquals(firstItem.findViewById(R.id.item_task_edit_title).getVisibility(), View.GONE);
        assertEquals(firstItem.findViewById(R.id.item_task_edit_description).getVisibility(), View.GONE);
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

