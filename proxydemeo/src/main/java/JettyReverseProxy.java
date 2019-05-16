import jetty.MyProxy;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * use jetty to implement the proxy
 */
public class JettyReverseProxy {
    private static void reverseProxy() throws Exception {
        Server server = new Server();


        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(9999);

        server.setConnectors(new Connector[]{connector});

        // Setup proxy handler to handle CONNECT methods
        ConnectHandler proxy = new ConnectHandler();
        server.setHandler(proxy);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler(proxy, "/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(MyProxy.class);

        // a dummy conf...
        // we will redirect by request on MyProxy
        proxyServlet.setInitParameter("proxyTo", "https://www.baidu.com/");
        //proxyServlet.setInitParameter("prefix", "/");
        context.addServlet(proxyServlet, "/*");

        server.start();
    }

    public static void main(String[] args) throws Exception {
        reverseProxy();
    }


}
