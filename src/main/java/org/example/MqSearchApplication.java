package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.config.Constants;
import org.example.manager.InterActionManager;
import org.example.manager.MQManager;
import org.example.manager.ResourceManager;
import org.example.manager.SearchManager;
import org.example.model.SearchSession;
import org.example.ui.MenuController;

import java.io.IOException;

public class MqSearchApplication {
    static JsonNode queueConfig = null;
    static JsonNode queueManagerConfig = null;
    static MQManager mqManager = MQManager.getInstance();
    static ResourceManager resourceManager = ResourceManager.getInstance();
    static InterActionManager interActionManager = InterActionManager.getInstance();
    static SearchManager searchManager = SearchManager.getInstance();
    static SearchSession searchSession = searchManager.createNewSession();

    public void run() {
        try {
            queueConfig = resourceManager.readJSON(Constants.Filenames.QUEUE_CONFIG);
            queueManagerConfig = resourceManager.readJSON(Constants.Filenames.QUEUE_MANAGER_CONFIG);
            mqManager.setQueueManagerConfig(queueManagerConfig);
        } catch (IOException e) {
            System.out.println("[ERROR] Unable to load configuration files, exiting...");
            System.exit(1);
        }

        searchSession.setEnvironment(Constants.Environments.DEFAULT_ENV);

        while (true) {
            MenuController.getInstance().showMainMenu(searchSession.getEnvironment(), searchSession.getTargetList());
            String userInput = interActionManager.getUserInput("Select an option and press [ENTER]: ");
            switch (userInput) {
                case "a" -> searchManager.addToTargetList(searchSession, queueConfig, queueManagerConfig);
                case "c" -> searchManager.clearTargetList(searchSession);
                case "l" -> searchManager.addToTargetListFromFile(searchSession, queueConfig, queueManagerConfig, searchSession.getEnvironment());
                case "s" -> searchManager.searchMessage(searchSession, queueConfig, queueManagerConfig);
                case "e" -> {
                    searchSession = searchManager.createNewSession();
                    searchManager.setTargetEnvironment(searchSession);
                }
                case "m" -> mqManager.seedTestMessages();
                case "t" -> mqManager.testQueueManagerConnection();
                case "q" -> {
                    System.out.println("[INFO] Exiting...");
                    System.exit(0);
                }
                default -> System.out.printf("[ERROR] Invalid option '%s'%n", userInput);
            }
        }
    }
}
