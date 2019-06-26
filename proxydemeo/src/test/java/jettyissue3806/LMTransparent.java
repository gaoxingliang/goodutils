package jettyissue3806;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LMTransparent extends ProxyServlet.Transparent {

    @Override
    protected HttpClient newHttpClient() {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true); // trust all
        HttpClient client = new HttpClient(sslContextFactory); // trust all
        return client;
    }

    @Override
    protected void onResponseContent(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, byte[] buffer,
                                     int offset, int length, Callback callback) {
        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "a mocked error here");
            callback.succeeded();
        }
        catch (Throwable e) { // make sure the callback is always called
            callback.failed(e);
        }

    }
}