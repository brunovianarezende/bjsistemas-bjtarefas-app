package nom.bruno.tasksapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.getsentry.raven.android.Raven;

public class TasksApplication extends Application {
    private TasksAppActivityLifecycleCallbacks mLifecycleCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        mLifecycleCallbacks = new TasksAppActivityLifecycleCallbacks();
        registerActivityLifecycleCallbacks(mLifecycleCallbacks);
        LogWrapper.configure(getApplicationContext());
    }

    public boolean isAppVisible() {
        return mLifecycleCallbacks.getNumVisibleActivities() > 0;
    }

    private static class TasksAppActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {
        private int mNumVisibleActivities = 0;

        public int getNumVisibleActivities() {
            return mNumVisibleActivities;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            mNumVisibleActivities++;
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mNumVisibleActivities--;
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
