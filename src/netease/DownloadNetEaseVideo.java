package netease;

import org.apache.http.HttpException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 下载网易视频的源码.
 * 测试url:
 *
 * todo:
 *  1. 添加cli支持
 *  2. 添加debug 模式 也就是logger支持
 *  3. 下载字幕支持
 *
 * @date latest update 04/02/2018
 */
public class DownloadNetEaseVideo {

    /**
     * @param args
     * @throws IOException
     * @throws HttpException
     */
    public static void main(String[] args) throws IOException {
        //use jsoup
        String url = "http://v.163.com/special/opencourse/algorithms.html?username=718596512@qq.com";
        String url2 = "http://open.163.com/special/opencourse/machinelearning.html";

        Document doc = Jsoup.connect(url2).get();
        // 选出课程列表
        Elements els = doc.select("td.u-ctitle");
        for (int i = 0; i < els.size(); i++) {
            Element e = els.get(i);
            NetworkVideo v = new NetworkVideo();
            v.indexName = ((TextNode) e.childNodes().get(0)).getWholeText().trim();
            Element anode = e.getElementsByTag("a").get(0);
            v.videoUrl = anode.attr("href");
            v.videoName = ((TextNode)anode.childNodes().get(0)).getWholeText().trim();
            /**
             * e is like:
             * <td class="u-ctitle"> [第11集]
             *  <a href="http://open.163.com/movie/2008/1/L/M/M6SGF6VB4_M6SGKG5LM.html">贝叶斯统计正则化</a>
             *  <img src="http://img1.cache.netease.com/v/2011/1414.png" class="isyy">
             * </td>
             */
            //System.out.println(v);
            dealOnePage(v);
        }

    }

    //对前面在项目页面获取的url进行处理 获取最终的视频 下载地址 并下载
    private static void dealOnePage(NetworkVideo video) throws IOException {
        Document doc = Jsoup.connect(video.videoUrl).get();
        //System.out.println(doc);

        int nodeIndex = -1;
        Elements els = doc.getElementsByTag("script");
        for (int i = 0; i < els.size(); i++) {
            if (els.get(i).childNodes().size() > 0) {
                if (els.get(i).childNodes().get(0).toString().contains("获取数据")) {
                    nodeIndex = i;
                    break;
                }
            }
        }
        if (nodeIndex >= 0) {
            DataNode dataNode = (DataNode)els.get(nodeIndex).childNodes().get(0);
            String script = dataNode.getWholeData();
            /**
             * search this string:
             *       // 当前movie信息
             _oc.getCurrentMovie = function() {
             return {
             id : 'M6SGHFBMC',
             number : 1,
             image : 'http://vimg1.ws.126.net' + '/image/snapshot_movie/2011/9/2/P/M7CTJ3D2P' + '.jpg',
             title : '机器学习的动机与应用',
             appsrc : 'http://mov.bn.netease.com/open-movie/nos/mp4/2015/01/19/SAFD8B131_sd.m3u8',
             src : 'http://swf.ws.126.net/openplayer/v01/-0-2_M6SGF6VB4_M6SGHFBMC-vimg1_ws_126_net//image/snapshot_movie/2011/9/2/P/M7CTJ3D2P-1430711943278.swf',
             jsUrl : 'http://live.ws.126.net/scores/M/C/M6SGHFBMC.js'
             };
             };
             */
            int index = script.indexOf("getCurrentMovie");
            int appSrcStartIndex = script.indexOf("appsrc", index + 10);
            int appSrcEndIndex = script.indexOf(",", appSrcStartIndex + 10);
            String appSrc = script.substring(appSrcStartIndex + 10, appSrcEndIndex-1).trim(); // no start '  and end '
            // https://mov.bn.netease.com/open-movie/nos/flv/2015/01/19/SAFD9VGEO_sd.flv
            String flvDownloadUrl = appSrc.replace("/mp4/", "/flv/").replace("m3u8", "flv");
            System.out.println(flvDownloadUrl);
            video.downloadUrl = flvDownloadUrl;
        }
        else {
            System.out.println("No video download url found");
        }

    }

    //下载单个文件 这里可以优化为多线程 但是意义不大 取决于网络状况；未考虑校验情况和下载失败情况
    private static void downfile(String url, String destPath, String fileName) {
        URL urlfile = null;
        HttpURLConnection httpUrl = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File f = new File(destPath + File.separator + fileName);
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
                System.out.println("read:" + total / 1024 + "KB");
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

    /**
     * 单个视频的对象
     */
    static class NetworkVideo {
        String indexName; //  [第10集]
        String videoName; // 特征选择
        String videoUrl; // http://open.163.com/movie/2008/1/U/O/M6SGF6VB4_M6SGJURUO.html
        String downloadUrl; // 真实下载地址
        List<String> srt = new ArrayList<>(); // 字幕文件

        @Override
        public String toString() {
            return "Video{" +
                    "indexName='" + indexName + '\'' +
                    ", videoName='" + videoName + '\'' +
                    ", videoUrl='" + videoUrl + '\'' +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }
    }

}
