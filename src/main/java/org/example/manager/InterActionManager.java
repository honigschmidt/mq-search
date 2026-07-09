package org.example.manager;

import java.util.Scanner;

public class InterActionManager {
    private static final InterActionManager INSTANCE = new InterActionManager();

    private InterActionManager() {}

    public static InterActionManager getInstance() {
        return INSTANCE;
    }

    public String getUserInput(String message) {
        String userInput;
        System.out.print(message);
        Scanner scanner = new Scanner(System.in);
        do {
            userInput = scanner.nextLine();
        } while (userInput == null);
        return userInput;
    }
}
