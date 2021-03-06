package model.worker;

/**
 * Created by rakshit on 13/03/2018.
 */
public final class Idle extends WorkerStatus {
    private static Idle instance;

    private Idle() {
    }

    public static Idle getInstance() {
        if (instance == null) {
            instance = new Idle();
        }
        return instance;
    }

    @Override
    public boolean isIdle() {
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
