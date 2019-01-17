package network.tcp;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 演示TCP RESET 在socket被关闭后 接着发送数据
 */
public class TcpResetDemo {
    static ServerSocket sc;

    public static void main(String[] args) throws Exception {

        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sc = new ServerSocket(7788);

                    Socket t = sc.accept();
                    System.out.println("New socket comming " + t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        server.start();

        Thread.sleep(1000);
        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", 7788));
        System.out.println("Connected");
        s.getOutputStream().write(new byte[1024]);
        s.getOutputStream().flush();
        sc.close();
        System.out.println("Server closed ");
        Thread.sleep(100);
        s.getOutputStream().write(new byte[10]);
        s.getOutputStream().flush();

        s.close();
    }

}
