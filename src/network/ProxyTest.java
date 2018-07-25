package network;

import org.apache.commons.cli.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;


/**
 * a simple proxy test
 * Example:
 * ProxyTest -http -h baidu.com -p 80 -pp 8089 -ph 192.168.170.148 -pu myuserWxithPass123456 -pa 123456
 *
 * ssl:
 * ProxyTest -http -s -h baidu.com -p 443 -pp 8089 -ph 192.168.170.148 -pu myuserWithPass123456 -pa 123456
 *
 */
public class ProxyTest {

    private static final Options options = new Options();

    static {
        options.addRequiredOption("ph", "proxy.host", true, "The proxy host");
        options.addRequiredOption("pp", "proxy.port", true, "The proxy port");
        options.addOption("pu", "proxy.user", true, "The proxy user");
        options.addOption("pa", "proxy.pass", true, "The proxy pass");
        options.addOption("pt", "proxy,type", true, "The proxy type: socks, http");

        options.addRequiredOption("h", "host", true, "The real host to test");
        options.addRequiredOption("p", "port", true, "The real host port to test");
        options.addOption("s", "ssl", false, "Connect to the real host using ssl?");
        options.addOption("http", "http", false, "try to read data when connected to the real host by url http(s)://host:port/");

        options.addOption("help", "help", false, "Show help");
        options.addOption("v", "v", false, "Show help");

    }

    private static void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ProxyTest", options);
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            help();
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        }
        catch (Exception e) {
            e.printStackTrace();
            help();
            return;
        }

        String h = cmd.getOptionValue("h");
        int p = Integer.valueOf(cmd.getOptionValue("p"));
        String user = cmd.getOptionValue("pu", "");
        String pass = cmd.getOptionValue("pa", "");

        String proxyHost = cmd.getOptionValue("ph");
        int proxtPort = Integer.valueOf(cmd.getOptionValue("pp"));
        String type = cmd.getOptionValue("pt", "http");
        if (!(type.equalsIgnoreCase("http") || type.equalsIgnoreCase("socks"))) {
            System.out.println("Invalid proxy type - " + type);
            help();
            return;
        }

        if (!user.isEmpty()) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass.toCharArray());
                }
            });
        }

        boolean ssl = cmd.hasOption("s");
        boolean http = cmd.hasOption("http"); // read data for http?

        Proxy proxy = new Proxy(type.equalsIgnoreCase("http") ? Proxy.Type.HTTP : Proxy.Type.SOCKS,
                InetSocketAddress.createUnresolved(proxyHost, proxtPort));
        System.out.println(String.format("Connect to %s:%s by proxy %s", h, p, proxy));

        Socket outgoing = new Socket(proxy);
        try {
            outgoing.setSoTimeout(1000 * 10);
            outgoing.connect(new InetSocketAddress(h, p), (int) TimeUnit.SECONDS.toMillis(10));
            if (ssl) {
                SSLSocket sslSocket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(outgoing, h, p, true);
                sslSocket.startHandshake();
                outgoing = sslSocket;
            }
            System.out.println("Connected");

            if (http) {
                System.out.println("Continue to read data");
                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        outgoing.getOutputStream())));

                out.println("GET / HTTP/1.1");
                out.println();
                out.flush();
                System.out.println("Request send");

                /*
                 * Make sure there were no surprises
                 */
                if (out.checkError()) {
                    System.out.println("SSLSocketClient:  java.io.PrintWriter error");
                    return;
                }

                /* read response */
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                outgoing.getInputStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Read data:" + inputLine);
                }

                in.close();
                out.close();
                System.out.println("Stream closed");
            }
        }
        catch (Exception e) {
            System.out.println("Connect failed");
            e.printStackTrace();
        }
        finally {
            if (outgoing != null) {
                try {
                    outgoing.close();
                }
                catch (IOException e) {
                }
            }
        }
    }
}
