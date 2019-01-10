package io.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class InputUtil {
    private InputUtil(){}

    private static Scanner _KEYBOARD = new Scanner(new BufferedReader(new InputStreamReader(System.in)));

    public static String getLine(String prompt) {
        String res = null;
        while (res == null || res.isEmpty()) {
            System.out.print(prompt);
            res = _KEYBOARD.nextLine().trim();
            if (!res.isEmpty()) {
                return res;
            }
        }
        return null;
    }
}
