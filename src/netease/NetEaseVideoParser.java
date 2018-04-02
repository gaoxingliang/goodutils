package netease;

import network.Network;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetEaseVideoParser implements VideoParser {
    private static final Logger LOGGER = LogManager.getLogger(NetEaseVideoParser.class);

    @Override
    public boolean accept(String url) throws Exception {
        return new URL(url).getHost().contains("163.com");
    }

    @Override
    public List<NetworkVideo> parse(String url) throws Exception {
        List<NetworkVideo> videos = new ArrayList<>();
        Document doc = Jsoup.connect(url).get();
        // 选出课程列表
        Elements els = doc.select("td.u-ctitle");
        for (int i = 0; i < els.size(); i++) {
            Element e = els.get(i);
            NetworkVideo v = new NetworkVideo();
            v.indexName = ((TextNode) e.childNodes().get(0)).getWholeText().trim();
            Element anode = e.getElementsByTag("a").get(0);
            v.videoUrl = anode.attr("href");
            v.videoName = ((TextNode) anode.childNodes().get(0)).getWholeText().trim();
            /**
             * e is like:
             * <td class="u-ctitle"> [第11集]
             *  <a href="http://open.163.com/movie/2008/1/L/M/M6SGF6VB4_M6SGKG5LM.html">贝叶斯统计正则化</a>
             *  <img src="http://img1.cache.netease.com/v/2011/1414.png" class="isyy">
             * </td>
             */
            dealOnePage(v);
            videos.add(v);
            LOGGER.info("The video is - " + v);
        }
        return videos;
    }

    //对前面在项目页面获取的url进行处理 获取最终的视频 下载地址 并下载
    private static void dealOnePage(NetworkVideo video) throws IOException, ParserConfigurationException, SAXException {
        Document doc = Jsoup.connect(video.videoUrl).get();
        LOGGER.info("Start parsing url " + video.videoName);
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
            DataNode dataNode = (DataNode) els.get(nodeIndex).childNodes().get(0);
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
             src : 'http://swf.ws.126.net/openplayer/v01/-0-2_M6SGF6VB4_M6SGHFBMC-vimg1_ws_126_net//image/snapshot_movie/2011/9/2/P
             /M7CTJ3D2P-1430711943278.swf',
             jsUrl : 'http://live.ws.126.net/scores/M/C/M6SGHFBMC.js'
             };
             };
             */
            int index = script.indexOf("getCurrentMovie");
            int appSrcStartIndex = script.indexOf("appsrc", index + 10);
            int appSrcEndIndex = script.indexOf(",", appSrcStartIndex + 10);
            String appSrc = script.substring(appSrcStartIndex + 10, appSrcEndIndex - 1).trim(); // no start '  and end '
            // https://mov.bn.netease.com/open-movie/nos/flv/2015/01/19/SAFD9VGEO_sd.flv
            String flvDownloadUrl = appSrc.replace("/mp4/", "/flv/").replace("m3u8", "flv");
            LOGGER.info("\t The download url is " + flvDownloadUrl);
            video.downloadUrl = flvDownloadUrl;

            /**
             * 获取jsUrl
             */
            int jsUrlStartIndex = script.indexOf("jsUrl", index + 10);
            int jsUrlEndIndex = script.indexOf(".js", jsUrlStartIndex + 10);
            String jsUrl = script.substring(jsUrlStartIndex + 9, jsUrlEndIndex);


            /**
             * 解析下载字幕的URL:
             * 从上面的src得到:2_M6SGF6VB4_M6SGHFBMC -> 然后访问:
             * https://live.ws.126.net/movie/B/O/2_M6SGF6VB4_M6SGHFBMC.xml
             * 得到xml
             */
            int srcStartIndex = script.indexOf("src", appSrcEndIndex + 1);
            int srcEndIndex = script.indexOf(".swf", srcStartIndex + 10);
            String src = script.substring(srcStartIndex + 7, srcEndIndex);
            LOGGER.debug("\t the sub src is - " + src);
            Pattern subpattern = Pattern.compile("(.*)(\\d_\\w+?_\\w+?)-(.*)");
            Matcher m = subpattern.matcher(src);
            if (m.matches()) {
                String subCore = m.group(2);
                String replaceJsUrl = jsUrl.replace("scores", "movie").replace("http", "https");
                replaceJsUrl = replaceJsUrl.substring(0, replaceJsUrl.lastIndexOf("/"));
                String subUrl = replaceJsUrl + "/" + subCore + ".xml";
                // https://live.ws.126.net/movie/M/C/2_M6SGF6VB4_M6SGHFBMC.xml
                String subResponse = Network.httpGet(subUrl, "GBK");
                LOGGER.info("The sub response is\n" + subResponse);
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                org.w3c.dom.Document subDoc = builder.parse(new ByteArrayInputStream(subResponse.getBytes("GBK")));
                // get the subs
                Node subsNode = subDoc.getChildNodes().item(0).getChildNodes().item(9);
                for (int i = 0; i < subsNode.getChildNodes().getLength(); i ++) {
                    String localeSubName = subsNode.getChildNodes().item(i).getChildNodes().item(0).getTextContent();
                    String localeSubUrl = subsNode.getChildNodes().item(i).getChildNodes().item(1).getTextContent();
                    LOGGER.info(String.format("\t subName-%s, url=%s", localeSubName, localeSubUrl));
                    video.srts.add(new String[]{localeSubName, localeSubUrl});
                }
            }
            else {
                LOGGER.error("The pattern is not found for src " + src);
            }
        }
        else {
            LOGGER.error("No video download url found for video - " + video);
        }

    }
}
