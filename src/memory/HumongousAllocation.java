package memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 检查G1GC的超大对象分配情况
 * -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xmx64m -XX:+UseG1GC -XX:G1HeapRegionSize=1m
 *
 */
public class HumongousAllocation {

    public static void main(String[] args) throws InterruptedException {
        // 使用region 大小是 1MB的来试试.
        while (true) {
            generateObjects();
            Thread.sleep(100);
        }
    }

    static List<BigObject> _datas = new ArrayList<>();

    private static void _process(BigObject buf) {
        _datas.add(buf);
        if (_datas.size() > 30) {
            _datas.clear();
        }
    }

    private static void generateObjects() {
        BigObject bigObject = new BigObject(1024 * 512);
        _process(bigObject);
    }

    static class BigObject {
        byte [] _buf;

        public BigObject(int bytes) {
            _buf = new byte[bytes];
        }
    }
}
