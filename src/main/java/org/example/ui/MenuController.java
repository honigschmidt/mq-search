package org.example.ui;

import org.example.config.Constants;

import java.util.List;

public class MenuController {
    private static final MenuController INSTANCE = new MenuController();

    private MenuController() {}

    public static MenuController getInstance() {return INSTANCE;}

    public void showMainMenu(String currentEnvironment, List<String> queueNameList) {
        System.out.printf("%n----------------%n");
        System.out.printf("MQ-Search v%s%n", Constants.Config.APP_VERSION);
        System.out.printf("----------------%n");
        System.out.printf("Environment: %s [selectable: %s]%n", currentEnvironment, Constants.Environments.ENVIRONMENT_LIST);
        System.out.print("Target list: ");
        if (queueNameList.isEmpty()) {
            System.out.print("empty");
        } else {
            queueNameList.forEach(qName -> System.out.printf("'%s' ", qName));
            System.out.printf("(%d)", queueNameList.size());
        }
        System.out.printf("%n----------------%n");
        System.out.printf("Target Configuration:%n");
        System.out.printf(" [a] Add queues manually%n");
        System.out.printf(" [l] Import queue names from file%n");
        System.out.printf(" [c] Clear target list%n");
        System.out.printf("Execution:%n");
        System.out.printf(" [s] Run queue search%n");
        System.out.printf(" [t] Test connection to queue manager%n");
        System.out.printf(" [m] Seed test messages (DEV environment only)%n");
        System.out.printf("System:%n");
        System.out.printf(" [e] Switch environment%n");
        System.out.printf(" [q] Exit application%n");
        System.out.printf("----------------%n");
    }
}
