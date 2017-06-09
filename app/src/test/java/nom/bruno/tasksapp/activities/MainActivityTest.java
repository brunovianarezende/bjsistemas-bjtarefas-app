package nom.bruno.tasksapp.activities;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowMotionEvent;

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
import nom.bruno.tasksapp.models.TaskUpdate;
import nom.bruno.tasksapp.services.TaskService;
import nom.bruno.tasksapp.services.TaskServiceStub;
import nom.bruno.tasksapp.shadows.ShadowToolbarActionBar;
import nom.bruno.tasksapp.view.adapters.BaseItemTouchHelperCallback;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowToolbarActionBar.class})
public class MainActivityTest {
    private static TestScheduler myScheduler = new TestScheduler();

    @BeforeClass
    public static void setupClass() {
        // make the moveThreshold lower so that drag and drop in Robolectric works as expected
        BaseItemTouchHelperCallback.moveThreshold = 0.1f;
    }

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
        startSelectMultipleItemsState(firstItem);
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
        assertEditStateItems(firstItem);

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
        assertEditStateItems(item);

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
        assertEditStateItems(firstItem);

        // cancel edition
        firstItem.findViewById(R.id.item_task_cancel_edit).performClick();
        myScheduler.triggerActions();
        assertItemSelectedStateItems(firstItem);

        // force a screen update (it could have happened because 30 seconds have passed)
        activity.forceTasksUpdate();
        myScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        myScheduler.triggerActions();

        // the state of the item must not have changed
        assertItemSelectedStateItems(firstItem);
    }

    @SuppressLint("SetTextI18n")
    @Test
    public void testItemBeingEditedUpdatedByOtherUser() {
        // make sure there will be an item in the list
        TaskService ts = Singletons.getTaskService(null);
        TaskCreation tc = new TaskCreation();
        tc.setTitle("t");
        tc.setDescription("d");
        int taskId = ts.addTask(tc).blockingFirst();

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
        // change title and description in the interface
        ((EditText) firstItem.findViewById(R.id.item_task_edit_title)).setText("New title");
        myScheduler.triggerActions();

        // meanwhile, some other user changed the content of the task
        TaskUpdate tu = new TaskUpdate();
        tu.setTitle("my title");
        tu.setDescription("my description");
        ts.updateTask(taskId, tu).blockingFirst();

        // then the screen is updated
        activity.forceTasksUpdate();
        myScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        myScheduler.triggerActions();

        // and the content of the edited item must not be updated
        firstItem = rvTasks.getChildAt(0);
        assertEquals(((EditText) firstItem.findViewById(R.id.item_task_edit_title)).getText().toString(), "New title");
        assertEquals(((EditText) firstItem.findViewById(R.id.item_task_edit_description)).getText().toString(), "d");

        // although if I cancel the edition, the new content must be there
        firstItem.findViewById(R.id.item_task_cancel_edit).performClick();
        myScheduler.triggerActions();
        assertEquals(((TextView) firstItem.findViewById(R.id.item_task_title)).getText(), "my title");
        assertEquals(((TextView) firstItem.findViewById(R.id.item_task_description)).getText(), "my description");

        // and if I edit it again, the new content must be there too
        firstItem.findViewById(R.id.item_task_edit).performClick();
        myScheduler.triggerActions();
        assertEquals(((EditText) firstItem.findViewById(R.id.item_task_edit_title)).getText().toString(), "my title");
        assertEquals(((EditText) firstItem.findViewById(R.id.item_task_edit_description)).getText().toString(), "my description");
    }

    @Test
    public void testDragAndDropItemSelectedState() {
        TaskService ts = Singletons.getTaskService(null);
        TaskCreation tc = new TaskCreation();
        int numItems = 5;
        for (int i = 0; i < numItems; i++) {
            tc.setTitle("t" + i);
            tc.setDescription("d" + i);
            ts.addTask(tc).blockingFirst();
        }

        // setup activity
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        myScheduler.triggerActions();

        // get item
        View selectedItem = getItem(activity, numItems - 1);

        // check if icon appear in item selected state
        selectItem(selectedItem);
        assertItemSelectedStateItems(selectedItem);


        // move last item to third position
        View targetItem = getItem(activity, 2);
        View moveButton = selectedItem.findViewById(R.id.item_task_move);
        dragItemBeforeTarget(moveButton, targetItem, selectedItem);

        // check final order of items
        int[] expected = new int[]{0, 1, 4, 2, 3};
        for (int i = 0; i < numItems; i++) {
            View item = getItem(activity, i);
            assertTitle(item, "t" + expected[i]);
            assertDescription(item, "d" + expected[i]);
        }

        // check if the API call was correct
    }

    @Test
    public void testReorderIsRobustAgainstServerFailures() {
        // check what happens if there is a server failure after API call to do reorder
    }

    private void assertTitle(View item, String title) {
        assertEquals(((TextView) item.findViewById(R.id.item_task_title)).getText(), title);
    }

    private void assertDescription(View item, String description) {
        assertEquals(((TextView) item.findViewById(R.id.item_task_description)).getText(), description);
    }

    private void dragItemBeforeTarget(View touchItemToTriggerDragAnDrop, View targetItem, View selectedItem) {
        int centerX = triggerDragAndDrop(touchItemToTriggerDragAnDrop);

        RecyclerView rvTasks = (RecyclerView) selectedItem.getParent();

        // the recycler view must also react to the initial touch action
        dispatchTouchEvent(rvTasks, MotionEvent.ACTION_DOWN, centerX, selectedItem.getTop());
        // this initial ACTION_MOVE call is needed to initialise some internal states
        dispatchTouchEvent(rvTasks, MotionEvent.ACTION_MOVE, centerX, selectedItem.getTop());

        // now we move the item up to the target, one item by one
        int targetPosition = rvTasks.getChildLayoutPosition(targetItem);
        int selectedItemPosition = rvTasks.getChildLayoutPosition(selectedItem);
        for (int currentPosition = selectedItemPosition - 1; currentPosition >= targetPosition; currentPosition--) {
            View currentItem = rvTasks.getChildAt(currentPosition);
            int target = currentItem.getTop() - 1;
            dispatchTouchEvent(rvTasks, MotionEvent.ACTION_MOVE, centerX, target);
        }
    }

    private void dragItemAfterTarget(View touchItemToTriggerDragAnDrop, View targetItem, View selectedItem) {
        int centerX = triggerDragAndDrop(touchItemToTriggerDragAnDrop);

        RecyclerView rvTasks = (RecyclerView) selectedItem.getParent();

        // the recycler view must also react to the initial touch action
        dispatchTouchEvent(rvTasks, MotionEvent.ACTION_DOWN, centerX, selectedItem.getTop());
        // this initial ACTION_MOVE call is needed to initialise some internal states
        dispatchTouchEvent(rvTasks, MotionEvent.ACTION_MOVE, centerX, selectedItem.getTop());

        // now we move the item down to the target, one item by one
        int targetPosition = rvTasks.getChildLayoutPosition(targetItem);
        int selectedItemPosition = rvTasks.getChildLayoutPosition(selectedItem);
        for (int currentPosition = selectedItemPosition + 1; currentPosition <= targetPosition; currentPosition++) {
            View currentItem = rvTasks.getChildAt(currentPosition);
            int target = currentItem.getTop() + 1;
            dispatchTouchEvent(rvTasks, MotionEvent.ACTION_MOVE, centerX, target);
        }
    }

    private int triggerDragAndDrop(View touchItemToTriggerDragAnDrop) {
        // start the drag and drop action. The action that will trigger it must be a touch
        int[] triggerCoordinates = new int[2];
        touchItemToTriggerDragAnDrop.getLocationInWindow(triggerCoordinates);
        int left = triggerCoordinates[0];
        int top = triggerCoordinates[1];
        int centerX = left + touchItemToTriggerDragAnDrop.getWidth() / 2;
        int centerY = top + touchItemToTriggerDragAnDrop.getHeight() / 2;
        dispatchTouchEvent(touchItemToTriggerDragAnDrop, MotionEvent.ACTION_DOWN, centerX, centerY);
        // dispatch an ACTION_CANCEL event just to be consistent with the run time behaviour
        // when we use ItemTouchHelper
        dispatchTouchEvent(touchItemToTriggerDragAnDrop, MotionEvent.ACTION_CANCEL, centerX, centerY);
        return centerX;
    }

    private void dispatchTouchEvent(View view, int action, int x, int y) {
        view.dispatchTouchEvent(ShadowMotionEvent.obtain(0, 100, action, x, y, 0));
        myScheduler.triggerActions();
    }

    private void selectItem(View item) {
        item.performClick();
        myScheduler.triggerActions();
    }

    private void startSelectMultipleItemsState(View item) {
        item.performLongClick();
        myScheduler.triggerActions();
    }

    private View getItem(MainActivity activity, int itemPosition) {
        RecyclerView rvTasks = (RecyclerView) activity.findViewById(R.id.tasks_recycler_view);
        assertNotNull(rvTasks);
        View item = rvTasks.getChildAt(itemPosition);
        assertNotNull(item);
        return item;
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

    private void assertEditStateItems(View item) {
        assertEquals(item.findViewById(R.id.item_task_edit).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_delete).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_move).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_title).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_description).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_edit_title).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_edit_description).getVisibility(), View.VISIBLE);
    }

    private void assertItemSelectedStateItems(View item) {
        assertEquals(item.findViewById(R.id.item_task_edit).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_delete).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_move).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_title).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_description).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_edit_title).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_edit_description).getVisibility(), View.GONE);
    }

    private void assertSelectMultipleItemsStateItems(View item) {
        assertEquals(item.findViewById(R.id.item_task_edit).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_delete).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_move).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_title).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_description).getVisibility(), View.VISIBLE);
        assertEquals(item.findViewById(R.id.item_task_edit_title).getVisibility(), View.GONE);
        assertEquals(item.findViewById(R.id.item_task_edit_description).getVisibility(), View.GONE);
    }
}

