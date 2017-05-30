package nom.bruno.tasksapp.androidservices;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.NoSuchElementException;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import nom.bruno.tasksapp.ExceptionHandler;
import nom.bruno.tasksapp.LogWrapper;
import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.TasksApplication;
import nom.bruno.tasksapp.activities.MainActivity;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TasksDelta;
import nom.bruno.tasksapp.services.TaskService;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NotificationService extends JobService {
    private static final int JOB_ID = 1;
    private static final int NOTIFICATION_ID = 1;

    @Override
    public boolean onStartJob(final JobParameters params) {
        // add exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        NotificationService.scheduleJob(this);
        final TasksApplication app = (TasksApplication) getApplication();
        if (app.isAppVisible()) {
            return false;
        } else {
            TaskService ts = TaskService.getInstance(app);
            ts
                    .getTasksDelta()
                    .subscribe(new Observer<TasksDelta>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull TasksDelta tasksDelta) {
                            try {
                                showTasksNotification(tasksDelta);
                            } catch (Exception e) {
                                LogWrapper.error(e);
                            } finally {
                                jobFinished(params, false);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if (!(e instanceof NoSuchElementException)) {
                                LogWrapper.error(e);
                            }
                            jobFinished(params, false);
                        }

                        @Override
                        public void onComplete() {
                            jobFinished(params, false);
                        }
                    });
            return true;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void showTasksNotification(TasksDelta tasksDelta) {
        if (tasksDelta.isEmpty()) {
            return;
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_task_notification)
                        .setAutoCancel(true)
                        // in my device, a lenovo, lights aren't been shown, I still need to
                        // understand why
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);

        if (tasksDelta.getTotalNumberOfChanges() == 1 && tasksDelta.getNewTasks().size() == 1) {
            Task task = tasksDelta.getNewTasks().get(0);
            mBuilder.setContentTitle(task.getTitle());
            mBuilder.setContentText(task.getDescription());
        } else {
            String tasksUpdatedPrefix = getString(R.string.notification_tasks_updated);
            mBuilder.setContentTitle(tasksUpdatedPrefix + ": " + tasksDelta.getTotalNumberOfChanges());
        }
        // for some reason the number is not showing...
        mBuilder.setNumber(tasksDelta.getTotalNumberOfChanges());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public static void scheduleJob(Context context) {
        // NOTE: this scheduling must be recalled when the job is finished to make sure it will
        // execute periodically. We won't create the job periodically because it won't behave
        // as desired: https://github.com/evernote/android-job/issues/26 and
        // https://developer.android.com/reference/android/app/job/JobInfo.Builder.html#setPeriodic(long)
        ComponentName serviceComponent = new ComponentName(context, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
                    .setMinimumLatency(30 * 1000) // wait at least
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // need network connection
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        }
    }
}
