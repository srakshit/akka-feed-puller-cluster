package model.protocol;

import java.io.Serializable;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class Ack implements Serializable {
    public final String feedName;

    public Ack(String feedName) {
        this.feedName = feedName;
    }

    @Override
    public String toString() {
        return "Ack {" + "feedName='" + feedName + "}";
    }
}
