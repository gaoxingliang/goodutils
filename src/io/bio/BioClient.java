package io.bio;

import io.common.InputUtil;
import io.common.ServerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * 最基础的BIO 客户端
 */
public class BioClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket(InetAddress.getByName(ServerInfo.HOST), ServerInfo.PORT);
        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
        scanner.useDelimiter("\n");
        while (true) {
            String line = InputUtil.getLine("Input something:").trim();
            line += "\n";
            socket.getOutputStream().write(line.getBytes());
            if (line.equalsIgnoreCase("bye")) {
                break;
            }
            System.out.println(scanner.nextLine());

        }
        socket.close();
    }
}
