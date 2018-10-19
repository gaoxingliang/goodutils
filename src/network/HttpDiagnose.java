package network;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Date;

/**
 * a simple class to compare the wget output and our java output
 * if the status code is not expected (use -Dcode=200 to set). will diagnose it.
 */
public class HttpDiagnose {
    /**
     *
     * @param args [0] for url
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {

        int expectedCode = Integer.getInteger("code", 200);
        int failedTimes = 0;
        while (true) {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(args[0]);
            try {
                System.out.println("Starting ..." + new Date());
                HttpResponse r = client.execute(get);
                if (r.getStatusLine().getStatusCode() != expectedCode) {
                    System.out.println("Status:" + r.getStatusLine());
                    try {
                        System.out.println("Content:" + IOUtils.toString(r.getEntity().getContent()));
                    }
                    catch (Exception e) {
                        System.out.println("Fail to get content");
                        e.printStackTrace();
                    }
                    throw new IllegalArgumentException("Unexepected code " + r.getStatusLine());
                }
                else {
                    System.out.println("Success - " + r.getStatusLine());
                }

            }
            catch (Exception e) {
                System.out.println("Fail to request " + args[0]);
                e.printStackTrace();
                failedTimes++;

                System.out.println("Using wget to try....");
                Process p = Runtime.getRuntime().exec(new String[]{"wget", "--debug", "-a", "output.txt", args[0]});
                p.waitFor();
                System.out.println("WgetOutput:");
                System.out.println(IOUtils.toString(p.getInputStream()));
                System.out.println("WgetError:");
                System.out.println(IOUtils.toString(p.getErrorStream()));
                p.destroy();
                if (failedTimes > 3) {
                    System.out.println("Exit it now");
                    System.exit(1);
                }
            }
            finally {
                System.out.println("Ending ...\n");
                Thread.sleep(1000 * 30);
            }
        }
    }
}
