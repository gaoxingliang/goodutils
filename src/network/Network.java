package network;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {
    public static String httpGet(String url, String encoding) {
        URL urlfile = null;
        HttpURLConnection httpUrl;
        BufferedInputStream bis = null;
        StringWriter sw = new StringWriter();
        try {
            urlfile = new URL(url);
            httpUrl = (HttpURLConnection) urlfile.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            int len = 0;
            int total = 0;
            byte[] b = new byte[1024];
            while ((len = bis.read(b)) != -1) {
                sw.write(new String(b, 0, len, encoding));
                total += len;
            }
            bis.close();
            httpUrl.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sw.toString();
    }



    //下载单个文件 这里可以优化为多线程 但是意义不大 取决于网络状况；未考虑校验情况和下载失败情况
    public static void downloadFile(String url, String outputFile) {
        URL urlfile = null;
        HttpURLConnection httpUrl = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File f = new File(outputFile);
        try {
            urlfile = new URL(url);
            httpUrl = (HttpURLConnection) urlfile.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(f));
            int len = 0;
            int total = 0;
            byte[] b = new byte[4096];
            while ((len = bis.read(b)) != -1) {
                bos.write(b, 0, len);
                total += len;
            }
            bos.flush();
            bis.close();
            httpUrl.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                bis.close();
                bos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
