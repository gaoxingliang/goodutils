package memory;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * run it with -agentlib:hprof=heap=sites  BigHeapTest
 */
public class BigHeapTest {
    public static void main(String[] args) throws Exception {
        createTask();
    }


    public static void createTask() throws InterruptedException {
        List list = new LinkedList<>();
        while (true) {
            Thread.sleep(new Random().nextInt(3000) + 1000);
            list.add(new String("Hello"));
        }
    }
}
