package org.example.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.mq.MQException;
import org.example.config.Constants;
import org.example.model.QueueSearchStats;
import org.example.model.SearchSession;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchManager {
    private static final SearchManager INSTANCE = new SearchManager();

    private SearchManager() {
    }

    public static SearchManager getInstance() {
        return INSTANCE;
    }

    public SearchSession createNewSession() {
        return new SearchSession();
    }

    public void setTargetEnvironment(SearchSession searchSession) {
        while (true) {
            String env = InterActionManager.getInstance().getUserInput("Enter environment (press [ENTER] without input to cancel): ");
            if (env.isBlank()) {
                break;
            }
            if (Constants.Environments.ENVIRONMENT_LIST.contains(env)) {
                searchSession.setEnvironment(env);
                break;
            } else {
                System.out.printf("[ERROR] Invalid environment '%s'%n", env);
            }
        }
    }

    public void addToTargetListFromFile(SearchSession searchSession, JsonNode queueConfig, JsonNode queueManagerConfig, String targetEnvironment) {
        String fileName = InterActionManager.getInstance().getUserInput("Enter file name to import (press [ENTER] without input to cancel). File must be in '" + Constants.Directories.WORKING_DIR + "' directory: ");
        if (!fileName.isBlank()) {
            List<String> importedList = ResourceManager.getInstance().importQueueNamesFromFile(fileName, queueConfig, queueManagerConfig, targetEnvironment);
            if (importedList != null && !importedList.isEmpty()) {
                importedList.forEach(qName -> {
                    if (!searchSession.getTargetList().contains(qName)) {
                        searchSession.getTargetList().add(qName);
                    }
                });
            }
        }
    }

    public void addToTargetList(SearchSession searchSession, JsonNode queueConfig, JsonNode queueManagerConfig) {
        AtomicInteger addCounter = new AtomicInteger();
        String queueName = InterActionManager.getInstance().getUserInput("Enter a full or partial queue name, then press [ENTER]: ").toUpperCase();
        if (!queueName.isBlank()) {
            Iterator<String> queueConfigIterator = queueConfig.fieldNames();
            queueConfigIterator.forEachRemaining(queueConfigEntry -> {
                if (queueConfigEntry.contains(queueName)) {
                    if (ResourceManager.getInstance().isQueueNameValid(queueConfigEntry, queueConfig, queueManagerConfig, searchSession.getEnvironment()) && !searchSession.getTargetList().contains(queueConfigEntry)) {
                        System.out.printf("[INFO] Added '%s' to target list%n", queueConfigEntry);
                        searchSession.getTargetList().add(queueConfigEntry);
                        addCounter.getAndIncrement();
                    }
                }
            });
        }
        System.out.printf("[INFO] Added %d queue(s) to target list%n", addCounter.get());
    }

    public void clearTargetList(SearchSession searchSession) {
        searchSession.getTargetList().clear();
    }

    public static int getTotalMatchCount(SearchSession searchSession) {
        int matchCount = 0;
        for (QueueSearchStats queueSearchStats : searchSession.getQueueSearchStatsList()) {
            matchCount += queueSearchStats.getMatchCount();
        }
        return matchCount;
    }

    public static int getTotalMessageCount(SearchSession searchSession) {
        int totalCount = 0;
        for (QueueSearchStats queueSearchStats : searchSession.getQueueSearchStatsList()) {
            totalCount += queueSearchStats.getMessageCount();
        }
        return totalCount;
    }

    public void searchMessage(SearchSession searchSession, JsonNode queueConfig, JsonNode queueManagerConfig) {
        if (searchSession.getTargetList().isEmpty()) {
            System.out.println("[ERROR] No queues have been selected");
        } else {
            searchSession.getSearchParameters().clear();
            for (int i = 0; i < Constants.Config.MAX_SEARCH_PARAMS; i++) {
                String searchParam = InterActionManager.getInstance().getUserInput("Enter a search parameter or leave blank and press [ENTER]: ");
                if (!searchParam.isBlank()) {
                    searchSession.getSearchParameters().add(searchParam);
                }
            }
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Constants.Config.SEARCH_DT_FORMAT);
            boolean isTimeStampValid = false;
            while (!isTimeStampValid) {
                String timeStamp = InterActionManager.getInstance().getUserInput("Enter search dates from/to (yyyyMMddHHmm-yyyyMMddHHmm) or leave blank and press [ENTER]. Shortcuts: [1] Last 15 minutes, [2] Last hour, [3] last 24 hours, [4] today: ");
                if (timeStamp.isBlank()) {
                    searchSession.setTimeFrom(null);
                    searchSession.setTimeTo(null);
                    isTimeStampValid = true;
                } else {
                    switch (timeStamp) {
                        case "1" -> {
                            searchSession.setTimeFrom(LocalDateTime.now().minusMinutes(15));
                            searchSession.setTimeTo(LocalDateTime.now());
                            isTimeStampValid = true;
                        }
                        case "2" -> {
                            searchSession.setTimeFrom(LocalDateTime.now().minusHours(1));
                            searchSession.setTimeTo(LocalDateTime.now());
                            isTimeStampValid = true;
                        }
                        case "3" -> {
                            searchSession.setTimeFrom(LocalDateTime.now().minusHours(24));
                            searchSession.setTimeTo(LocalDateTime.now());
                            isTimeStampValid = true;
                        }
                        case "4" -> {
                            searchSession.setTimeFrom(LocalDate.now().atStartOfDay());
                            searchSession.setTimeTo(LocalDateTime.now());
                            isTimeStampValid = true;
                        }
                        default -> {
                            int fromStart = 0;
                            int fromEnd = 12;
                            int toStart = 13;
                            int toEnd = 25;
                            try {
                                searchSession.setTimeFrom(LocalDateTime.parse(timeStamp.substring(fromStart, fromEnd), dateTimeFormatter));
                                searchSession.setTimeTo(LocalDateTime.parse(timeStamp.substring(toStart, toEnd), dateTimeFormatter));
                                if (searchSession.getTimeTo().isAfter(searchSession.getTimeFrom())) {
                                    System.out.println("[ERROR] Start time after end time");
                                } else {
                                    isTimeStampValid = true;
                                }
                            } catch (DateTimeException e) {
                                System.out.println("[ERROR] Invalid timestamp format");
                            }
                        }
                    }
                }

                searchSession.getQueueSearchStatsList().clear();
                searchSession.setSessionStart(LocalDateTime.now());
                System.out.printf("%n[INFO] Search started%n");

                for (String queueName : searchSession.getTargetList()) {
                    searchSession.getQueueManagerList().clear();
                    JsonNode qmListNode = queueConfig.get(queueName).get(Constants.Config.QCONFIG_QMGR_LIST_NAME);
                    for (JsonNode qmNode : qmListNode) {
                        String qmEnv = queueManagerConfig.path(qmNode.asText()).path(Constants.Config.QMCONFIG_ENV_NAME).asText();
                        if (Objects.equals(qmEnv, searchSession.getEnvironment())) {
                            searchSession.getQueueManagerList().add(qmNode.asText());
                        }
                    }
                    for (String queueManagerName : searchSession.getQueueManagerList()) {
                        QueueSearchStats queueSearchStats = new QueueSearchStats(queueName, queueManagerName);
                        try {
                            int messageCount = MQManager.getInstance().getQueueDepth(queueName, queueManagerName);
                            queueSearchStats.setMessageCount(messageCount);
                        } catch (MQException e) {
                            System.out.printf("%s %s%n", Constants.Messages.ERROR_MQ_EXCEPTION, e.getMessage());
                        }
                        System.out.printf("[INFO] Checking '%s' (%s)%n", queueName, queueManagerName);
                        System.out.printf("[INFO] Messages in queue: %d%n", queueSearchStats.getMessageCount());
                        if (queueSearchStats.getMessageCount() != 0) {
                            MQManager.getInstance().browseQueue(queueName, queueManagerName, searchSession, queueSearchStats);
                        }
                        System.out.printf("[INFO] Matching messages in queue: %d%n", queueSearchStats.getMatchCount());
                        searchSession.addQueueSearchStats(queueSearchStats);
                    }
                }
                searchSession.setSessionEnd(LocalDateTime.now());
                System.out.println("[INFO] Search finished");
                DateTimeFormatter logDateTimeFormatter = DateTimeFormatter.ofPattern(Constants.Config.LOG_DT_FORMAT);
                System.out.printf("%n");
                System.out.printf("--- MQ-Search v%s ---%n", Constants.Config.APP_VERSION);
                System.out.printf("%-13s %s%n", "Environment: ", searchSession.getEnvironment());
                System.out.printf("%-13s %s%n", "Started: ", searchSession.getSessionStart().format(logDateTimeFormatter));
                System.out.printf("%-13s %s%n", "Finished: ", searchSession.getSessionEnd().format(logDateTimeFormatter));
                System.out.printf("%-13s %.3f s%n", "Duration: ", Duration.between(searchSession.getSessionStart(), searchSession.getSessionEnd()).toMillis() / 1000.0);
                System.out.printf("---%n");
                System.out.printf("%-13s %s%n", "Search: ", (searchSession.getSearchParameters().isEmpty() ? "*" : searchSession.getSearchParameters()));
                System.out.printf("%-13s %s%n", "Time from: ", (Objects.isNull(searchSession.getTimeFrom()) ? "*" : searchSession.getTimeFrom().format(logDateTimeFormatter)));
                System.out.printf("%-13s %s%n", "Time to: ", (Objects.isNull(searchSession.getTimeTo()) ? "*" : searchSession.getTimeTo().format(logDateTimeFormatter)));
                System.out.printf("---%n");
                String firstColumnHead = "Matches";
                String secondColumnHead = "Queue (QMGR)";
                System.out.printf("%-13s %s%n", firstColumnHead, secondColumnHead);
                System.out.printf("%-13s %s%n", "-------", "------------");
                List<QueueSearchStats> queueSearchStatsList = searchSession.getQueueSearchStatsList();
                for (QueueSearchStats queueSearchStats : queueSearchStatsList) {
                    String firstColumnValue = "[" + queueSearchStats.getMatchCount() + "/" + queueSearchStats.getMessageCount() + "]";
                    String secondColumnValue = queueSearchStats.getQueueName() + "(" + queueSearchStats.getQueueManagerName() + ")";
                    System.out.printf("%-13s %s%n", firstColumnValue, secondColumnValue);
                }
                System.out.printf("TOTAL: [%s/%s]%n", getTotalMatchCount(searchSession), getTotalMessageCount(searchSession));
                System.out.printf("---%n");
            }
        }
    }
}
