package org.example.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.Constants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class ResourceManager {
    private static final ResourceManager INSTANCE = new ResourceManager();

    private ResourceManager() {
        this.createWorkingDir();
    }

    public static ResourceManager getInstance() {
        return INSTANCE;
    }

    public void createWorkingDir() {
        Path workingDirPath = Paths.get(Constants.Directories.WORKING_DIR);
        try {
            Files.createDirectories(workingDirPath);
        } catch (IOException e) {
            System.out.printf("%s %s%n", Constants.Messages.ERROR_IO_EXCEPTION, e);
            System.exit(1);
        }
    }

    public String readFile(String fileName) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException();
        }
        return Files.readString(Path.of(Constants.Directories.WORKING_DIR, fileName), StandardCharsets.UTF_8);
    }

    public void writeFile(String fileName, String content) throws IOException {
        if (fileName == null || content == null) {
            throw new IllegalArgumentException();
        }
        Path path = Path.of(Constants.Directories.WORKING_DIR, fileName);
        Files.writeString(path, content);
        System.out.printf("[INFO] File '%s' written%n", path);
    }

    public JsonNode readJSON(String resourceName) throws IOException {
        if (resourceName == null) {
            throw new IllegalArgumentException();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IOException();
        }
        try (inputStream) {
            return objectMapper.readTree(inputStream);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public String readString(String resourceName) throws IOException {
        if (resourceName == null) {
            throw new IllegalArgumentException();
        }
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new FileNotFoundException();
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    public List<String> importQueueNamesFromFile(String fileName, JsonNode queueConfig, JsonNode queueManagerConfig, String currentEnvironment) {
        List<String> queuesFromFile = new ArrayList<>();
        List<String> results = new ArrayList<>();
        try {
            String fileContent = readFile(fileName);
            Scanner scanner = new Scanner(fileContent);
            while (scanner.hasNextLine()) {
                queuesFromFile.add(scanner.nextLine());
            }
            scanner.close();
        } catch (IOException e) {
            System.out.printf("[ERROR] File '%s' not found%n", fileName);
            return null;
        }
        if (!queuesFromFile.isEmpty()) {
            results = new ArrayList<>();
            for (String queueFromFile : queuesFromFile) {
                if (isQueueNameValid(queueFromFile, queueConfig, queueManagerConfig, currentEnvironment)) {
                    results.add(queueFromFile.trim());
                }
            }
        } else {
                System.out.printf("[ERROR] The file '%s' contains no valid queue names for import%n", fileName);
        }
        return results;
    }

    public Boolean isQueueNameValid(String queueName, JsonNode queueConfig, JsonNode queueManagerConfig, String currentEnvironment) {
        JsonNode queueConfigNode = queueConfig.get(queueName);
        if (queueConfigNode != null) {
            JsonNode queueManagerList = queueConfigNode.get(Constants.Config.QCONFIG_QMGR_LIST_NAME);
            for (JsonNode queueManager : queueManagerList) {
                String queueManagerEnvironment = queueManagerConfig.path(queueManager.asText()).path(Constants.Config.QMCONFIG_ENV_NAME).asText();
                if (Objects.equals(queueManagerEnvironment, currentEnvironment)) {
                    return true;
                }
            }
        }
        return false;
    }
}
