package org.buildcli.utils.tools;

import java.util.Scanner;

public class CLIInteractions {

    public static boolean getConfirmation(String string) {
        String action = String.format("Do you wanna %s? (Y/N) ", string);

        Scanner scanner = new Scanner(System.in);
        System.out.println(action);
        String answer = scanner.nextLine().trim().toUpperCase();

        while (!answer.equals("Y") && !answer.equals("N")) {
            System.out.println("Invalid answer. " +action);
            answer = scanner.nextLine().trim().toUpperCase();
        }

        scanner.close();
        return answer.equalsIgnoreCase("y");
    }
}
