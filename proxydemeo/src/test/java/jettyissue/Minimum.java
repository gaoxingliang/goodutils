package jettyissue;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Minimum {


    @Test
    public void sslErrorHidden() throws Exception {

        // for (int i =0; i < 10; i++) {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true);

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

            }

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

            }
        }}, null);
        sslContextFactory.setSslContext(sslContext);


        HttpClient jetty;
        jetty = new HttpClient(sslContextFactory);
        jetty.start();

        String someSelfSignedHttpsServer = "https://127.0.0.1:7443";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        jetty.newRequest(someSelfSignedHttpsServer)
                .method(HttpMethod.POST)
                .send(result -> {
                    if (result.isFailed()) {
                        error.set(result.getFailure());
                    }
                    latch.countDown();
                });


        latch.await();
        System.out.println("Error found " + error.get());
    }
}