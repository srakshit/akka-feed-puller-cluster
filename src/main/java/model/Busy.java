package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public final class Busy extends WorkerStatus {
    private final String workId;

    private Busy(String workId) {
        this.workId = workId;
    }

    @Override
    protected boolean isIdle() {
        return false;
    }

    @Override
    protected String getWorkId() {
        return workId;
    }

    @Override
    public String toString() {
        return "Busy: {work=" + workId + "}";
    }
}
