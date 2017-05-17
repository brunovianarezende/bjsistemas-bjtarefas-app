package nom.bruno.tasksapp;

import android.content.Context;
import android.util.Log;

import com.getsentry.raven.android.Raven;

public class LogWrapper {
    public static void configure(Context context) {
        getLogger().init(context);
    }

    public static void error(Throwable t) {
        getLogger().error(t);
    }

    private static HowToLog getLogger() {
        if (Constants.SENTRY_DSN == null) {
            return new LogToLog();
        } else {
            return new LogToSentry();
        }
    }

    private interface HowToLog {
        void init(Context context);

        void error(Throwable t);
    }

    private static class LogToSentry implements HowToLog {
        @Override
        public void init(Context context) {
            Raven.init(context, Constants.SENTRY_DSN);
        }

        @Override
        public void error(Throwable t) {
            Raven.capture(t);
        }
    }

    private static class LogToLog implements HowToLog {

        @Override
        public void init(Context context) {

        }

        @Override
        public void error(Throwable t) {
            Log.e("error", "error", t);
        }
    }
}
