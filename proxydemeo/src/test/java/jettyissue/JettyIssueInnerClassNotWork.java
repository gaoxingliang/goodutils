package jettyissue;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * https://github.com/eclipse/jetty.project/issues/3821
 */
public class JettyIssueInnerClassNotWork {

    private static final int PORT = 9999;
    private static final String DUMMY_ADDR = "https://google.com";
    public static final String LOCAL_ADDR = "0.0.0.0";


    public static void main(String[] args) throws Exception {
        startServer();
    }


    public static void startServer() throws Exception {

        // let's listening on local port
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(LOCAL_ADDR);
        connector.setPort(PORT);

        server.setConnectors(new Connector[]{connector});


        // Setup proxy handler to handle CONNECT methods
        ConnectHandler proxy = new ConnectHandler();
        server.setHandler(proxy);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler(proxy, "/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(LMTransparent.class);

        // a dummy conf...
        proxyServlet.setInitParameter("proxyTo", DUMMY_ADDR);
        context.addServlet(proxyServlet, "/*");
        server.start();
    }

    public static class LMTransparentStatic extends ProxyServlet.Transparent {

        public LMTransparentStatic() {

        }

        @Override
        protected HttpClient newHttpClient() {
            SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true); // trust all
            HttpClient client = new HttpClient(sslContextFactory); // trust all
            return client;
        }

    }
    public class LMTransparent extends ProxyServlet.Transparent {

        public LMTransparent() {

        }

        @Override
        protected HttpClient newHttpClient() {
            SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true); // trust all
            HttpClient client = new HttpClient(sslContextFactory); // trust all
            return client;
        }

    }



}
