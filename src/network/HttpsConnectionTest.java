package network;

import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.net.URLConnection;

public class HttpsConnectionTest {
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Require the url arg.");
            return;
        }
        boolean ignoreSSL = Boolean.getBoolean("ignoreSSL");
        if (ignoreSSL) {
            /**
            SSLUtils.disableSSLHostnameVerification();
            SSLUtils.disableHttpsCertVerification();
             */
        }
        String urlStr = args[0];
        URL url = new URL(urlStr);
        System.out.println(url);
        URLConnection c = url.openConnection();
        String output = IOUtils.toString(c.getInputStream());
        System.out.println(output);
    }
}
