package netease;

import java.util.List;

public interface VideoParser {
    boolean accept(String url) throws Exception;
    List<NetworkVideo> parse(String url) throws Exception;
}
