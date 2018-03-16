package model.protocol;

import java.io.Serializable;

/**
 * Created by rakshit on 16/03/2018.
 */

public final class WorkIsReady implements Serializable {
    private static final WorkIsReady instance = new WorkIsReady();
    public static WorkIsReady getInstance() {
        return instance;
    }
}
