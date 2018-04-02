package netease;

import network.Network;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * 下载网易视频的源码.
 * 测试url:
 * <p>
 * todo:
 * 1. 添加cli支持
 * 2. 添加debug 模式 也就是logger支持
 * 3. 下载字幕支持
 *
 * @date latest update 04/02/2018
 */
public class DownloadNetEaseVideo {
    private static final Logger LOGGER = LogManager.getLogger(DownloadNetEaseVideo.class);

    /**
     * @param args
     * @throws IOException
     * @throws HttpException
     */
    public static void main(String[] args) throws Exception {
        //use jsoup
        String url = "http://v.163.com/special/opencourse/algorithms.html?username=718596512@qq.com";
        String url2 = "http://open.163.com/special/opencourse/machinelearning.html";
        NetEaseVideoParser parser = new NetEaseVideoParser();
        List<NetworkVideo> videos = parser.parse(url2);
        for (NetworkVideo v : videos) {
            System.out.println(v);
            System.out.println("Start downloading....");
            String destPrefix = v.indexName + "_" + v.videoName;
            Network.downloadFile(v.downloadUrl, destPrefix + ".flv");
            for (String [] localSrt : v.srts) {
                Network.downloadFile(localSrt[1], destPrefix + "_" + localSrt[0] + ".srt");
            }
        }
    }
}
