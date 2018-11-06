package network;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * a simple class to compare the wget output and our java output
 * if the status code is not expected (use -Dcode=200 to set). will diagnose it.
 * if the status code is an "expected error code" (use -DexpectError=403). will exit it
 *
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
        String expectedErrorCodeStr = System.getProperty("expectError", "");
        List<Integer> expectedErrorCodes = new ArrayList<>();
        if (!expectedErrorCodeStr.isEmpty()) {
            String [] codes = expectedErrorCodeStr.split(",");
            for (String c : codes) {
                expectedErrorCodes.add(Integer.valueOf(c));
            }
        }
        int failedTimes = 0;
        while (true) {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(args[0]);
            int code = 0;
            try {
                System.out.println("Starting ..." + new Date());
                HttpResponse r = client.execute(get);
                code = r.getStatusLine().getStatusCode();
                if (code != expectedCode) {
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
                if (code > 0 && expectedErrorCodes.size() > 0 &&  expectedErrorCodes.contains(code)) {
                    if (failedTimes > 3) {
                        System.out.println("The expected error found, Exit it now");
                        System.exit(1);
                    }
                }
                else {
                    System.out.println("Will continue even error found...");

                }
            }
            finally {
                System.out.println("Ending ...\n");
                Thread.sleep(1000 * 30);
            }
        }
    }
    static class ExpectedErrorException extends Exception {
        public int code;
    }

}
