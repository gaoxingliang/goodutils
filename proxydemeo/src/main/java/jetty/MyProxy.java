package jetty;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.Request;
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
    protected void copyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {
        super.copyHeaders(clientRequest, proxyRequest);
    }

    @Override
    protected ContentProvider proxyRequestContent(Request proxyRequest, HttpServletRequest request) throws IOException {
        return super.proxyRequestContent(proxyRequest, request);
    }

    @Override
    protected Response.Listener newProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
        return super.newProxyResponseListener(request, response);
    }

    @Override
    protected void onClientRequestFailure(Request proxyRequest, HttpServletRequest request, Throwable failure) {
        super.onClientRequestFailure(proxyRequest, request, failure);
    }

    @Override
    protected void onRewriteFailed(HttpServletRequest request, HttpServletResponse response) throws IOException {
        super.onRewriteFailed(request, response);
    }

    @Override
    protected void onResponseHeaders(HttpServletRequest request, HttpServletResponse response, Response proxyResponse) {
        super.onResponseHeaders(request, response, proxyResponse);
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
    protected void onResponseSuccess(HttpServletRequest request, HttpServletResponse response, Response proxyResponse) {
        super.onResponseSuccess(request, response, proxyResponse);
    }

    @Override
    protected void onResponseFailure(HttpServletRequest request, HttpServletResponse response, Response proxyResponse,
                                     Throwable failure) {
        super.onResponseFailure(request, response, proxyResponse, failure);
    }

    @Override
    protected URI rewriteURI(HttpServletRequest request) {
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

        return rewrittenURI;
    }

    @Override
    protected void customizeProxyRequest(Request proxyRequest, HttpServletRequest request) {
        super.customizeProxyRequest(proxyRequest, request);
    }

    @Override
    protected String filterResponseHeader(HttpServletRequest request, String headerName, String headerValue) {
        return super.filterResponseHeader(request, headerName, headerValue);
    }
}
