package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public abstract class WorkerStatus {
    public abstract boolean isIdle();

    public boolean isBusy() {
        return !isIdle();
    }

    protected abstract String getWorkId();
}
