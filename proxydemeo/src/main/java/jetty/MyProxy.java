package jetty;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * an example of how we redirect different requests to different backends
 */
public class MyProxy extends  ProxyServlet.Transparent {
    public MyProxy() {
        super();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (Boolean.valueOf(request.getHeader("first"))) {
            System.out.println("It's first request......");
            System.out.println("Re-write it to another request....");
            super.service(request, response);
        }
        else {
            super.service(request, response);
        }
    }

    @Override
    protected HttpClient newHttpClient() {
        return new HttpClient(new SslContextFactory());
    }

    @Override
    protected String filterServerResponseHeader(HttpServletRequest clientRequest, Response serverResponse, String headerName,
                                                String headerValue) {
        return super.filterServerResponseHeader(clientRequest, serverResponse, headerName, headerValue);
    }

    @Override
    protected void onResponseContent(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, byte[] buffer,
                                     int offset, int length, Callback callback) {
        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
    }


    @Override
    protected String rewriteTarget(HttpServletRequest request) {

        String _prefix = "/", _proxyTo = "";
        if (request.getQueryString() != null && request.getQueryString().contains("testme")) {
            _proxyTo = "https://www.baidu.com/";
            System.out.println("Redircting to another host...");
        }
        else {
            _proxyTo = "http://12306.com";
        }
        String path = request.getRequestURI();
        if (!path.startsWith(_prefix))
            return null;

        StringBuilder uri = new StringBuilder(_proxyTo);
        if (_proxyTo.endsWith("/"))
            uri.setLength(uri.length() - 1);
        String rest = path.substring(_prefix.length());
        if (!rest.startsWith("/"))
            uri.append("/");
        uri.append(rest);
        String query = request.getQueryString();
        if (query != null)
            uri.append("?").append(query);
        URI rewrittenURI = URI.create(uri.toString()).normalize();

        return rewrittenURI.toString();
    }

}
