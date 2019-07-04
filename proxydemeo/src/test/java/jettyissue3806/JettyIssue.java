package jettyissue3806;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * In this class, shows the issue:
 * https://github.com/eclipse/jetty.project/issues/3806
 * <p>
 * All exceptions and resource process is ignored here
 *
 * Requirements:
 *
 *
 * 1. port 9999 on local is open.
 * 2. access: http://127.0.0.1:9999/index
 *    on firefox and chrome >= 73
 *
 *
 */
public class JettyIssue {

    private static final int PORT = 9999;
    private static final String PROXY_ADDR = "https://google.com";
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
        server.setErrorHandler(new ExampleErrorHandler());

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler(proxy, "/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(LMTransparent.class);


        proxyServlet.setInitParameter("proxyTo", PROXY_ADDR);
        context.addServlet(proxyServlet, "/*");
        server.start();
    }


    public static class ExampleErrorHandler extends ErrorHandler {

        @Override
        protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request, HttpServletResponse response, int code,
                                                  String message, String mimeType) throws IOException {

            //baseRequest.getHttpFields().remove(HttpHeader.ACCEPT);
            //baseRequest.getHttpFields().add(HttpHeader.ACCEPT, MimeTypes.Type.TEXT_HTML.asString());
            super.generateAcceptableResponse(baseRequest, request, response, code, message, MimeTypes.Type.TEXT_HTML.asString());
            /**
             * We found the chrome added a Accept type since chrome 73.
             * and this cause the error message is not showing eg when a port is redirected from 80 to 443.
             * here we ignore the MIME type is okay. we really do return a HTML page
             */
        }

        @Override
        protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
            writer.write("We found an error - " + message + " with code - " + code);
        }
    }

}
