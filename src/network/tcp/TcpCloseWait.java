package network.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Close wait...
 * 被动关闭的一方没有及时处理收到的数据或者进行close操作.  就会有大量的CLOSE_WAIT状态.
 * MAC上执行: netstat -anvp tcp | grep CLOSE_WAIT | grep 8091
 *
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65014        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65013        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65012        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65011        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65010        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65009        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65008        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65007        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65006        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 * tcp4       0      0  127.0.0.1.8091         127.0.0.1.65005        CLOSE_WAIT  408295 146988  48855      0 0x0022 0x00000004
 *
 */
public class TcpCloseWait {

    public static void main(String[] args) throws InterruptedException {
        new Thread(new Server()).start();
        TimeUnit.SECONDS.sleep(3);
        for (int i = 0; i < 10; i++) {
            new Thread(new Client()).start();
        }
    }

    static class Client implements Runnable {

        @Override
        public void run() {
            Socket r = new Socket();
            try {
                r.connect(new InetSocketAddress("127.0.0.1", 8091));
            }
            catch (IOException e) {
                System.out.println("Not connected");
                return;
            }

            try {
                r.getOutputStream().write("hello".getBytes());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            try {
                r.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    static class Server implements Runnable {

        @Override
        public void run() {

            try {
                ServerSocket sc = new ServerSocket(8091);
                while (true) {
                    Socket newS = sc.accept();
                    new Thread(new Task(newS)).start();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class Task implements Runnable {
        Socket s;
        public Task(Socket s) {
            this.s = s;
        }

        @Override
        public void run() {
            byte [] buf = new byte[512];
            try {
                while (s.getInputStream().read(buf) > 0 ) {
                    // do nothing
                }
                // EOF
                // But we don't close it
                // 模拟被动关闭的一方 不及时关闭连接  或者一直在忙碌 的话 就会这样.
                Thread.sleep(TimeUnit.MINUTES.toMillis(3));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
