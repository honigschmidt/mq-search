package org.example.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateQueueConfig {

    static final String QM_CONFIG = "qmconfig.json";
    static final String Q_CONFIG = "qconfig.json";
    static final String QM_LIST = "qmgrList";
    static JsonNode queueManagerConfig;
    static JsonNode queueConfig;
    static AtomicInteger totalQCount = new AtomicInteger();
    static AtomicInteger qCount = new AtomicInteger();
    static void main(String[] args) {
        createQueueConfig(args);
    }

    public static void createQueueConfig(String [] args) {
        HashSet<String> queueNameFilters = (args != null && args.length > 0) ? new HashSet<>(Arrays.asList(args)) : new HashSet<>();
        try {
            queueManagerConfig = readJSON(QM_CONFIG);
        } catch (IOException e) {
            System.out.println("[ERROR] Unable to load configuration file, exiting...");
            System.exit(1);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        queueConfig = objectMapper.createObjectNode();
        List<String> queueManagerNames = new ArrayList<>();
        Iterator<String> qmConfigIterator = queueManagerConfig.fieldNames();
        JsonNode finalQueueManagerConfig = queueManagerConfig;
        qmConfigIterator.forEachRemaining(queueManagerName -> {
            if ((finalQueueManagerConfig.get(queueManagerName)).get("active").asBoolean()) {
                queueManagerNames.add(queueManagerName);
            }
        });
        System.out.printf("[INFO] Starting CreateQueueConfig with filter parameters %s...%n", Arrays.toString(args));
        for (String queueManagerName : queueManagerNames) {
            qCount.set(0);
            System.out.printf("[INFO] Connecting to '%s'...%n", queueManagerName);
            try {
                List<String> queueNames = getQueueNames(queueManagerName);
                for (String queueName : queueNames) {
                    if (isMatched(queueName, queueNameFilters)) {
                        if(queueConfig.has(queueName)) {
                            ArrayNode qmListNode = (ArrayNode) queueConfig.at("/" + queueName).get(QM_LIST);
                            List<String> qmList = new ArrayList<>();
                            for (JsonNode qmNode : qmListNode) {
                                qmList.add(qmNode.asText());
                            }
                            if (!qmList.contains(queueManagerName)) {
                                qmListNode.add(queueManagerName);
                            }
                        } else {
                            ObjectNode queueNode = objectMapper.createObjectNode();
                            ((ObjectNode) queueConfig).set(queueName, queueNode);
                            List<String> qmList = new ArrayList<>();
                            qmList.add(queueManagerName);
                            ArrayNode qmListNode = objectMapper.valueToTree(qmList);
                            queueNode.putArray(QM_LIST).addAll(qmListNode);
                        }
                        qCount.getAndIncrement();
                        totalQCount.getAndIncrement();
                    }
                }
            } catch (RuntimeException e) {
                System.out.printf("[ERROR] Unable to get queue list from '%s'%n", queueManagerName);
            }
            System.out.printf("[INFO] Queues added: %d%n", qCount.intValue());
        }
        System.out.printf("[INFO] Total queues added: %d%n", totalQCount.intValue());
        if (!queueConfig.isEmpty()) {
            try {
                writeFile(Q_CONFIG, prettyPrintJSON(objectMapper.writeValueAsString(queueConfig)));
            } catch (IOException e) {
                System.out.printf("[ERROR] Unable to write queue config, %s%n", e);
            }
        }
    }

    private static boolean isMatched(String queueName, HashSet<String> queueNameFilters) {
        if (queueNameFilters.isEmpty()) {
            return true;
        }
        for (String filter : queueNameFilters) {
            if (queueName.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> getQueueNames(String queueManagerName) {
        List<String> queueNames = new ArrayList<>();
        try {
            MQQueueManager queueManager = new MQQueueManager(queueManagerName, setConnectionProperties(queueManagerName));
            System.out.printf("[INFO] Connected to '%s', getting queue list...%n", queueManagerName);
            PCFMessageAgent pcfMessageAgent = new PCFMessageAgent(queueManager);
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, "*");
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            request.addParameter(CMQCFC.MQIACF_Q_ATTRS, new int[]{CMQC.MQCA_Q_NAME});
            PCFMessage[] responses = pcfMessageAgent.send(request);
            for (PCFMessage response : responses) {
                String queueName = response.getStringParameterValue(CMQC.MQCA_Q_NAME);
                queueNames.add(queueName.trim());
            }
            pcfMessageAgent.disconnect();
            queueManager.disconnect();
            return queueNames;
        }
        catch (MQException mqe) {
            System.out.printf("[ERROR] Unable to connect to the queue manager '%s'%n", queueManagerName);
        }
        catch (MQDataException | IOException mqdioe) {
            System.out.printf("[ERROR] Unable to get queue list from queue manager '%s'%n", queueManagerName);
        }
        return queueNames;
    }

    private static Hashtable<String, Object> setConnectionProperties(String queueManagerName) {
        Hashtable<String, Object> connectionProperties = new Hashtable<>();
        JsonNode connectionParameters = queueManagerConfig.path(queueManagerName);
        if (connectionParameters.isMissingNode()) {
            throw new IllegalArgumentException("[ERROR] Queue manager '" + queueManagerName + "' not found in configuration data");
        } else {
            connectionProperties.put(CMQC.HOST_NAME_PROPERTY, connectionParameters.get("host").asText());
            connectionProperties.put(CMQC.PORT_PROPERTY, connectionParameters.get("port").asInt());
            connectionProperties.put(CMQC.CHANNEL_PROPERTY, connectionParameters.get("channel").asText());
            connectionProperties.put(CMQC.USER_ID_PROPERTY, connectionParameters.get("user_id").asText());
            connectionProperties.put(CMQC.PASSWORD_PROPERTY, connectionParameters.get("password").asText());
        }
        return  connectionProperties;
    }

    private static JsonNode readJSON(String resourceName) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        return new ObjectMapper().readTree(inputStream);
    }

    private static String prettyPrintJSON(String unformattedJSON) throws JsonProcessingException {
        return new ObjectMapper().readTree(unformattedJSON).toPrettyString();
    }

    private static void writeFile(String fileName, String content) throws IOException {
        if (fileName == null || content == null) {
            throw new IllegalArgumentException();
        }
        Path path = Path.of(System.getProperty("user.home"), fileName);
        Files.writeString(path, content);
        System.out.printf("[INFO] File '%s' written%n", path);
    }
}