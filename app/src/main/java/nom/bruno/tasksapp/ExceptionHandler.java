package nom.bruno.tasksapp;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LogWrapper.error(e);
    }
}
