package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public final class Idle extends WorkerStatus {
    private static final Idle instance = new Idle();

    @Override
    protected boolean isIdle() {
        return true;
    }

    @Override
    protected String getWorkId() {
        throw new IllegalAccessError();
    }

    @Override
    public String toString() {
        return "Idle";
    }
}
