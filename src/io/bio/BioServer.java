package io.bio;

import io.common.ServerInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 最基础的BIO服务器端
 */
public class BioServer {
    public static void main(String[] args) throws Exception {

        ServerSocket ss = new ServerSocket(ServerInfo.PORT);
        System.out.println("Server started on " + ServerInfo.PORT);
        ExecutorService es = Executors.newCachedThreadPool();


        while (true) {
            Socket socket = ss.accept();
            es.submit(new TaskHandler(socket));
        }
    }

    static class TaskHandler implements Runnable {
        private Socket socket;
        private Scanner scanner;
        private BufferedWriter out;
        public TaskHandler(Socket socket) {
            this.socket = socket;
            try {
                scanner = new Scanner(socket.getInputStream());
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                String line = scanner.nextLine();
                System.out.println("Read from client - " + line);
                if (line != null) {
                    if (line.equalsIgnoreCase("bye")) {
                        break;
                    }
                    else {
                        try {
                            out.write("Echo - " + line + "\n");
                            out.flush();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            scanner.close();
            try {
                out.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
