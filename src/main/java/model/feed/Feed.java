package model.feed;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by rakshit on 10/03/2018.
 */
public class Feed implements Serializable {
    private int id;
    private String company;
    private String feedName;
    private String url;
    private boolean isActive;
    private boolean override;
    private long interval;
    private String backOff;

    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdated;

    public Feed() {

    }

    public Feed(Feed feed){
        this.id = feed.id;
        this.company = feed.company;
        this.feedName = feed.feedName;
        this.url = feed.url;
        this.lastUpdated = feed.lastUpdated;
        this.isActive = feed.isActive;
        this.override = feed.override;
        this.interval = feed.interval;
        this.backOff = feed.backOff;
    }

    public int getId() { return this.id; }

    public String getCompany() {
        return this.company;
    }

    public String getFeedName() {
        return this.feedName;
    }

    public String getUrl() {
        return this.url;
    }

    public boolean getIsActive() { return this.isActive; }

    public boolean getOverride() { return this.override; }

    public long getInterval() { return this.interval; }

    public String getBackOff() { return this.backOff; }

    public Date getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setOverride(boolean value) {
        this.override = value;
    }

    public void setBackOff(String backOff) {
        this.backOff = backOff;
    }

    @Override
    public String toString() {
        return "Feed: {company=" + company + ", feed=" + feedName + ", url=" + url + ", last_updated=" + lastUpdated + "}";
    }
}
