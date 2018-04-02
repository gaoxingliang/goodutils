package netease;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个视频的对象
 */
public class NetworkVideo {
    String indexName; //  [第10集]
    String videoName; // 特征选择
    String videoUrl; // http://open.163.com/movie/2008/1/U/O/M6SGF6VB4_M6SGJURUO.html
    String downloadUrl; // 真实下载地址
    List<String[]> srts = new ArrayList<>(); // 字幕文件 [en, https://xxx.src], [ch, https://.....srt]

    @Override
    public String toString() {
        return "Video{" +
                "indexName='" + indexName + '\'' +
                ", videoName='" + videoName + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", srts='" + _getSrts() + '\'' +
                '}';
    }

    private String _getSrts() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String[] srt : srts) {
            stringBuilder.append(srt[0]).append("=").append(srt[1]).append(",");
        }
        return stringBuilder.toString();
    }
}