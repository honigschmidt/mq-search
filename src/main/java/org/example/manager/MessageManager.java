package org.example.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.MQMessage;
import org.apache.commons.codec.binary.Hex;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.example.config.Constants;
import org.example.model.QueueSearchStats;
import org.example.model.SearchSession;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

public class MessageManager {
    private static final MessageManager INSTANCE = new MessageManager();

    private MessageManager() {}

    public static MessageManager getInstance() {
        return INSTANCE;
    }

    public void processMessage(MQMessage mqMessage, String queueName, SearchSession searchSession, QueueSearchStats queueSearchStats) throws IOException {
        byte[] byteMessage = new byte[mqMessage.getMessageLength()];
        String messageHeader = null;
        mqMessage.seek(0);
        String rfhStrucID = mqMessage.readStringOfByteLength(4);
        mqMessage.seek(8);

        try {
            int rfhStrucLength = mqMessage.readInt();
            if (rfhStrucID.trim().equalsIgnoreCase("rfh") || rfhStrucID.trim().equalsIgnoreCase("rfh2")) {
                mqMessage.seek(0);
                mqMessage.readFully(byteMessage);
                byte[] byteHeader = Arrays.copyOfRange(byteMessage, 0, rfhStrucLength);
                byteMessage = Arrays.copyOfRange(byteMessage, rfhStrucLength, mqMessage.getMessageLength());
                messageHeader = new String(byteHeader);
            } else {
                mqMessage.seek(0);
                mqMessage.readFully(byteMessage);
            }
        } catch (IOException e) {
            mqMessage.seek(0);
            mqMessage.readFully(byteMessage);
        }

        String messagePayload = new String(byteMessage);
        String messageId = Hex.encodeHexString(mqMessage.messageId);
        String correlationId = Hex.encodeHexString(mqMessage.correlationId);
        LocalDateTime messageTimeStamp = mqMessage.putDateTime.toZonedDateTime().toLocalDateTime().plusHours(timeOffset(Constants.Config.ZONE_ID));
        boolean isMessageMatch = false;
        boolean isSearchParamsFilled = (!searchSession.getSearchParameters().isEmpty());
        boolean isTimeStampsFilled = (!Objects.isNull(searchSession.getTimeFrom()) && !Objects.isNull(searchSession.getTimeTo()));
        boolean isPayloadJSON = false;
        boolean isPayloadXML = false;
        boolean isPayloadRaw = false;

        String parseTest;
        try {
            parseTest = prettyPrintXML(messagePayload);
            isPayloadXML = true;
        } catch (RuntimeException ignored) {}
        finally {
            try {
                String strippedPayload = messagePayload.substring(messagePayload.indexOf("{"));
                parseTest = prettyPrintJSON(strippedPayload);
                isPayloadJSON = true;
            } catch (RuntimeException ignored) {}
            finally {
                if (!isPayloadXML && !isPayloadJSON) {
                    isPayloadRaw = true;
                }
            }
        }

        if (!isSearchParamsFilled && !isTimeStampsFilled) {
            isMessageMatch = true;
        }
        if (isSearchParamsFilled && !isTimeStampsFilled) {
            for (String searchParameter : searchSession.getSearchParameters()) {
                if (messageId.contains(searchParameter) || correlationId.contains(searchParameter)) {
                    isMessageMatch = true;
                }
                if (Objects.nonNull(messageHeader)) {
                    if (messageHeader.contains(searchParameter.toLowerCase())) {
                        isMessageMatch = true;
                    }
                }
                if (messagePayload.toLowerCase().contains(searchParameter.toLowerCase()))
                {
                    isMessageMatch = true;
                }
            }
        }
        if (!isSearchParamsFilled && isTimeStampsFilled) {
            if (messageTimeStamp.isAfter(searchSession.getTimeFrom()) && messageTimeStamp.isBefore(searchSession.getTimeTo())) {
                isMessageMatch = true;
            }
        }
        if (isSearchParamsFilled && isTimeStampsFilled) {
            if (messageTimeStamp.isAfter(searchSession.getTimeFrom()) && messageTimeStamp.isBefore(searchSession.getTimeTo())) {
                for (String searchParameter : searchSession.getSearchParameters()) {
                    if (messageId.contains(searchParameter) || correlationId.contains(searchParameter)) {
                        isMessageMatch = true;
                    }
                    if (Objects.nonNull(messageHeader)) {
                        if (messageHeader.contains(searchParameter.toLowerCase())) {
                            isMessageMatch = true;
                        }
                    }
                    if (messagePayload.toLowerCase().contains(searchParameter.toLowerCase()))
                    {
                        isMessageMatch = true;
                    }
                }
            }
        }

        if (isMessageMatch) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSS");
            String fileTimeStamp = dateTimeFormatter.format(messageTimeStamp);
            String fileUID = messageId.substring(messageId.length() - 8);
            String fileName = queueName + "_" + fileTimeStamp + "_" + fileUID;
            if (isPayloadXML) {
                ResourceManager.getInstance().writeFile(fileName + Constants.Extensions.XML, prettyPrintXML(messagePayload));
            }
            else if (isPayloadJSON) {
                messagePayload = messagePayload.substring(messagePayload.indexOf("{"));
                ResourceManager.getInstance().writeFile(fileName + Constants.Extensions.JSON, prettyPrintJSON(messagePayload));
            }
            else if (isPayloadRaw) {
                ResourceManager.getInstance().writeFile(fileName + Constants.Extensions.TXT, messagePayload);
            }
            queueSearchStats.incrementMatchCount();
        }
    }

    public String prettyPrintXML(String unformattedXML) {
        try {
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setNewLineAfterDeclaration(false);
            org.dom4j.Document document = DocumentHelper.parseText(unformattedXML);
            StringWriter stringWriter = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(stringWriter, outputFormat);
            xmlWriter.write(document);
            return stringWriter.toString();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String prettyPrintJSON(String unformattedJSON) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(unformattedJSON).toPrettyString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer timeOffset(ZoneId zoneId) {
        if (zoneId.getRules().isDaylightSavings(Instant.now())) {
            return 2;
        } else {
            return 1;
        }
    }
}
