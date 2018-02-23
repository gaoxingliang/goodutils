import com.google.common.base.Splitter;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * a simple curl to download all data
 */
public class Handshaker {

    /**
     * @param args [host:port, sslprotocol, ciphers]
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("[host:port, sslprotocol, ciphers]");
            return;
        }
        String hostx = args[0];
        int portIndex = hostx.indexOf(":");
        int portx = 443;
        if (portIndex > 0) {
            portx = Integer.valueOf(hostx.substring(portIndex + 1));
            hostx = hostx.substring(0, portIndex);
        }
        final String host = hostx;
        final int port = portx;
        final String sslProtocol = args[1];
        final String[] sslCipherSuites = Splitter.on(",").omitEmptyStrings().splitToList(args[2]).toArray(new String[0]);
        SSLContext protocol = SSLContext.getInstance(sslProtocol);
        protocol.init(null, null, new SecureRandom());
        System.out.println("Use args:" + Arrays.toString(args));
        {

            System.out.println("Round one ---------");
            SSLSocket sslSocket = null;
            Exception e1 = null;
            try {
                sslSocket = (SSLSocket) protocol.getSocketFactory().createSocket(host, port);
                sslSocket.setEnabledProtocols(new String[]{sslProtocol});
                sslSocket.setEnabledCipherSuites(sslCipherSuites);
                sslSocket.setSoTimeout(30000);
                sslSocket.startHandshake();
                System.out.println("Handshake finished");
                System.out.println("Cipher is - " + sslSocket.getSession().getCipherSuite());
                System.out.println("Protocol is - " + sslSocket.getSession().getProtocol());

            }
            catch (Exception e) {
                e1 = e;
            }
            finally {
                if (sslSocket != null) {
                    sslSocket.close();
                }
            }
            if (e1 != null) {
                System.out.println(ExceptionUtils.getFullStackTrace(e1));
            }
        }
        System.out.println("Sleep 5s");
        Thread.sleep(5000);

        {

            System.out.println("Round two ---------");
            // ok let's try another
            SSLSocketFactory sf = SSLUtils.getSSLSocketFactory(sslProtocol,
                    new String[]{sslProtocol},
                    sslCipherSuites,
                    new SecureRandom(),
                    null);
            Exception e2 = null;
            Socket sock = new Socket();
            try {
                sock.connect(new InetSocketAddress(host, port), 30000);
                sock.setSoTimeout(30000);

                // Wrap plain socket in an SSL socket
                SSLSocket socket = (SSLSocket) sf.createSocket(sock, host, port, true);
                socket.startHandshake();

                System.out.println("Handshake finished");
                System.out.println("Cipher is - " + socket.getSession().getCipherSuite());
                System.out.println("Protocol is - " + socket.getSession().getProtocol());

            }
            catch (Exception e) {
                e2 = e;
            }
            finally {
                sock.close();
            }

            Thread.sleep(5000);

        }

        int test = args.length >= 4 ? Integer.valueOf(args[3]) : 20;
        ExecutorService es = Executors.newFixedThreadPool(test);
        for (int i = 0; i < test; i++) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        boolean suc = false;
                        try {
                            suc = handshakeTest(host, port, sslProtocol, sslCipherSuites);
                            System.out.println(Thread.currentThread().getId() + " Result - " + suc);
                            Thread.sleep(3000 + new Random().nextInt(1000) );

                        }
                        catch (Exception e) {
                            System.out.println(Thread.currentThread().getId() + " Result - false " + e.getMessage());
                        }


                    }
                }
            });
        }

    }


    public static boolean handshakeTest(String host, int port, String sslProtocol, String [] sslCipherSuites) throws Exception {
            // ok let's try another
            SSLSocketFactory sf = SSLUtils.getSSLSocketFactory(sslProtocol,
                    new String[]{sslProtocol},
                    sslCipherSuites,
                    new SecureRandom(),
                    null);
            Exception e2 = null;
            Socket sock = new Socket();
            try {
                sock.connect(new InetSocketAddress(host, port), 30000);
                sock.setSoTimeout(30000);

                // Wrap plain socket in an SSL socket
                SSLSocket socket = (SSLSocket) sf.createSocket(sock, host, port, true);
                socket.startHandshake();
                return true;
            }
            catch (Exception e) {
                throw e;
            }
            finally {
                sock.close();
            }
    }


    static class HandshakeResult {
        boolean success;
        String cipher, protocol;
    }
}
