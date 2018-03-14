package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public abstract class WorkerStatus {
    protected abstract boolean isIdle();

    private boolean isBusy() {
        return !isIdle();
    }

    protected abstract String getWorkId();
}
