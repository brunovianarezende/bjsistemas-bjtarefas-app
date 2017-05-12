package nom.bruno.tasksapp.androidservices;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import nom.bruno.tasksapp.R;
import nom.bruno.tasksapp.activities.MainActivity;
import nom.bruno.tasksapp.models.Task;
import nom.bruno.tasksapp.models.TasksDelta;

@TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
public class NotificationService extends JobService {
    private static final int JOB_ID = 1;
    private static final int NOTIFICATION_ID = 1;

    @Override
    public boolean onStartJob(JobParameters params) {
        TasksDelta delta = new TasksDelta();
        Task task = new Task();
        task.setTitle("Title");
        task.setDescription("description");
        delta.getNewTasks().add(task);
        showTasksNotification(delta);
        NotificationService.scheduleJob(this);
        return false;
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
                        .setAutoCancel(true);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
                    .setMinimumLatency(30 * 1000) // wait at least
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // need network connection
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        }
    }
}
