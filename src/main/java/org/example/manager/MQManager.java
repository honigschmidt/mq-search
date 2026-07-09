package org.example.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import org.example.config.Constants;
import org.example.model.QueueSearchStats;
import org.example.model.SearchSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class MQManager {
    private static final MQManager INSTANCE = new MQManager();
    private JsonNode queueManagerConfig;

    private MQManager() {}

    public static MQManager getInstance() {
        return INSTANCE;
    }

    public int getQueueDepth(String queueName, String queueManagerName)  throws MQException {
        MQQueueManager queueManager = new MQQueueManager(queueManagerName, setConnectionProperties(queueManagerName));
        int queueOpenOptions = MQConstants.MQOO_INQUIRE;
        MQQueue queue = queueManager.accessQueue(queueName, queueOpenOptions);
        int queueDepth = queue.getCurrentDepth();
        queue.close();
        queueManager.disconnect();
        return queueDepth;
    }

    public MQMessage getMQMessage(String queueName, String queueManagerName) throws MQException {
        MQQueueManager queueManager = new MQQueueManager(queueManagerName, setConnectionProperties(queueManagerName));
        int queueOpenOptions = MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_OUTPUT;
        MQQueue queue = queueManager.accessQueue(queueName, queueOpenOptions);
        MQGetMessageOptions getMessageOptions = new MQGetMessageOptions();
        MQMessage mqMessage = new MQMessage();
        queue.get(mqMessage, getMessageOptions);
        queue.close();
        queueManager.disconnect();
        return mqMessage;
    }

    public void putMQMessage(MQMessage mqMessage, String queueName, String queueManagerName) throws MQException {
        MQQueueManager queueManager = new MQQueueManager(queueManagerName, setConnectionProperties(queueManagerName));
        int queueOpenOptions = MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_OUTPUT;
        MQPutMessageOptions putMessageOptions = new MQPutMessageOptions();
        MQQueue queue = queueManager.accessQueue(queueName, queueOpenOptions);
        queue.put(mqMessage, putMessageOptions);
        queue.close();
        queueManager.disconnect();
    }

    public void browseQueue(String queueName, String queueManagerName, SearchSession searchSession, QueueSearchStats queueSearchStats) {
        try {
            boolean isFirstMessage = true;
            boolean isQueueFullyRead = false;
            MQQueueManager queueManager = new MQQueueManager(queueManagerName, setConnectionProperties(queueManagerName));
            int queueOpenOptions = MQConstants.MQOO_BROWSE;
            MQQueue queue = queueManager.accessQueue(queueName, queueOpenOptions);
            MQMessage mqMessage = new MQMessage();
            MQGetMessageOptions getMessageOptions = new MQGetMessageOptions();
            while (!isQueueFullyRead) {
                try {
                    if (isFirstMessage) {
                        getMessageOptions.options = MQConstants.MQGMO_BROWSE_FIRST + MQConstants.MQGMO_WAIT + CMQC.MQGMO_PROPERTIES_FORCE_MQRFH2;
                        isFirstMessage = false;
                    } else {
                        getMessageOptions.options = MQConstants.MQGMO_BROWSE_NEXT + MQConstants.MQGMO_WAIT + CMQC.MQGMO_PROPERTIES_FORCE_MQRFH2;
                    }
                    queue.get(mqMessage, getMessageOptions);
                    MessageManager.getInstance().processMessage(mqMessage, queueName, searchSession, queueSearchStats);
                    mqMessage.clearMessage();
                    mqMessage.correlationId = MQConstants.MQCI_NONE;
                    mqMessage.messageId = MQConstants.MQMI_NONE;
                } catch (MQException e) {
                    if (e.reasonCode == 2033) {
                        isQueueFullyRead = true;
                    } else {
                        System.out.printf("%s %s%n", Constants.Messages.ERROR_MQ_EXCEPTION, e.getMessage());
                    }
                }
            }
            queue.close();
            queueManager.disconnect();
        } catch (MQException e) {
            System.out.printf("%s %s%n", Constants.Messages.ERROR_MQ_EXCEPTION, e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testQueueManagerConnection() {
        String queueManagerName = InterActionManager.getInstance().getUserInput("Enter queue manager name (press [ENTER] without input to cancel): ");
        if (!queueManagerName.isBlank()) {
            MQQueueManager queueManager = null;
            try {
                queueManager = new MQQueueManager(queueManagerName, setConnectionProperties(queueManagerName));
                System.out.printf("[INFO] Connection to '%s' successful%n", queueManagerName);
            }
            catch (IllegalArgumentException iae) {
                System.out.printf("[ERROR] Invalid queue manager '%s'%n", queueManagerName);
            }
            catch (MQException mqe_conn) {
                System.err.printf("[ERROR] Connection to queue manager '%s' failed: %s%n", queueManagerName, mqe_conn.getMessage());
            } finally {
                if (queueManager != null && queueManager.isConnected()) {
                    try {
                        queueManager.disconnect();
                    } catch (MQException mqe_disconn) {
                        System.err.printf("[ERROR] Closing connection to queue manager '%s' failed: %s%n", queueManagerName, mqe_disconn.getMessage());
                    }
                }
            }
        }
    }

    public void seedTestMessages() {
        List<String> messageList = new ArrayList<>();
        try {
            messageList.add(ResourceManager.getInstance().readString(Constants.Filenames.TEST_MESSAGE_JSON));
            messageList.add(ResourceManager.getInstance().readString(Constants.Filenames.TEST_MESSAGE_XML));
            messageList.add(ResourceManager.getInstance().readString(Constants.Filenames.TEST_MESSAGE_TXT));
        } catch (IOException e) {
            System.out.printf("%s %s.%n", Constants.Messages.ERROR_IO_EXCEPTION, e);
        }
        for (String message : messageList) {
            MQMessage mqMessage = new MQMessage();
            try {
                mqMessage.writeString(message);
                putMQMessage(mqMessage, Constants.Config.TEST_Q_NAME, Constants.Config.TEST_QM_NAME);
                mqMessage.clearMessage();
                mqMessage.correlationId = MQConstants.MQCI_NONE;
                mqMessage.messageId = MQConstants.MQMI_NONE;
            } catch (IOException ioe) {
                System.out.printf("%s %s%n", Constants.Messages.ERROR_IO_EXCEPTION, ioe);
            } catch (MQException mqe) {
                System.out.printf("%s %s%n", Constants.Messages.ERROR_MQ_EXCEPTION, mqe.getMessage());
            }
        }
    }

    private Hashtable<String, Object> setConnectionProperties(String queueManagerName) {
        Hashtable<String, Object> connectionProperties = new Hashtable<>();
        JsonNode connectionParameters = queueManagerConfig.path(queueManagerName);
        if (connectionParameters.isMissingNode()) {
            throw new IllegalArgumentException("[ERROR] Queue manager '" + queueManagerName + "' not found in config data");
        } else {
            connectionProperties.put(CMQC.HOST_NAME_PROPERTY, connectionParameters.get("host").asText());
            connectionProperties.put(CMQC.PORT_PROPERTY, connectionParameters.get("port").asInt());
            connectionProperties.put(CMQC.CHANNEL_PROPERTY, connectionParameters.get("channel").asText());
            connectionProperties.put(CMQC.USER_ID_PROPERTY, connectionParameters.get("user_id").asText());
            connectionProperties.put(CMQC.PASSWORD_PROPERTY, connectionParameters.get("password").asText());
        }
        return  connectionProperties;
    }

    public void setQueueManagerConfig(JsonNode queueManagerConfig) {
        this.queueManagerConfig = queueManagerConfig;
    }
}
