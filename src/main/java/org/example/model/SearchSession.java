package org.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SearchSession {

    private String environment;
    private final List<String> targetList = new ArrayList<>();
    private final List<String> queueManagerList = new ArrayList<>();
    private final List<String> searchParameters = new ArrayList<>();
    private LocalDateTime timeFrom;
    private LocalDateTime timeTo;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private final List<QueueSearchStats> queueSearchStatsList = new ArrayList<>();

    public SearchSession() {}

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getTargetList() {
        return targetList;
    }

    public List<String> getSearchParameters() {
        return searchParameters;
    }

    public List<QueueSearchStats> getQueueSearchStatsList() {
        return queueSearchStatsList;
    }

    public LocalDateTime getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(LocalDateTime timeFrom) {
        this.timeFrom = timeFrom;
    }

    public LocalDateTime getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(LocalDateTime timeTo) {
        this.timeTo = timeTo;
    }

    public LocalDateTime getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(LocalDateTime sessionStart) {
        this.sessionStart = sessionStart;
    }

    public LocalDateTime getSessionEnd() {
        return sessionEnd;
    }

    public void setSessionEnd(LocalDateTime sessionEnd) {
        this.sessionEnd = sessionEnd;
    }

    public List<String> getQueueManagerList() {
        return queueManagerList;
    }

    public void addQueueSearchStats(QueueSearchStats queueSearchStats) {
        queueSearchStatsList.add(queueSearchStats);
    }
}
