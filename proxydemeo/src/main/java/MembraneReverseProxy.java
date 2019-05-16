
import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

/**
 * use 127.0.0.1:4000 to 12306.com
 */
public class MembraneReverseProxy {
    public static void main(String[] args) throws Exception {
        String hostname = "*";
        String method = "GET";
        String path = ".*";
        int listenPort = 4000;

        ServiceProxyKey key = new ServiceProxyKey(hostname, method, path, listenPort);

        String targetHost = "12306.com";
        int targetPort = 80;

        ServiceProxy sp = new ServiceProxy(key, targetHost, targetPort);

        HttpRouter router = new HttpRouter();
        router.add(sp);
        router.init();
    }
}
