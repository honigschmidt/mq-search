package org.example.model;

import java.util.concurrent.atomic.AtomicInteger;

public class QueueSearchStats {
    private final String queueName;
    private final String queueManagerName;
    private final AtomicInteger matchCount = new AtomicInteger();
    private int messageCount;

    public QueueSearchStats(String queueName, String queueManagerName) {
        this.queueName = queueName;
        this.queueManagerName = queueManagerName;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getQueueManagerName() {
        return queueManagerName;
    }

    public int getMatchCount() {
        return matchCount.get();
    }

    public void incrementMatchCount() {
        this.matchCount.getAndIncrement();
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}
