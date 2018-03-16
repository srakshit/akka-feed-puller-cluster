package model.worker;

/**
 * Created by rakshit on 13/03/2018.
 */
public final class Busy extends WorkerStatus {
    private final String workId;

    public Busy(String workId) {
        this.workId = workId;
    }

    @Override
    public boolean isIdle() {
        return false;
    }

    @Override
    protected String getWorkId() {
        return workId;
    }

    @Override
    public String toString() {
        return "Busy: {" + "work=" + workId + "}";
    }
}
