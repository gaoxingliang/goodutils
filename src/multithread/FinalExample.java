package multithread;

import java.util.concurrent.*;

/**
 * a multiple thread example to demo the semantic of final field
 *
 * Have test a lot of times, but didn't got what I expected exceptional case. :(
 */
public class FinalExample {
    int i;
    final int j;

    static FinalExample obj;

    static CountDownLatch cd;



    public FinalExample() {
        j = 2;
        Thread.yield();
        i = 1;
    }

    public static void writer() {
        obj = new FinalExample();
    }

    // sum
    // if it's null, return -1
    public static void reader() {

        FinalExample o = obj;
        if (o != null) {
            int a = o.i;  // a may not be initialized
            int b = o.j; // b must be 2
            if (a == 1 && b == 2) {

            } else {
                // find an exceptional case
                System.out.println(String.format("i=%s,j=%s", a, b));
                System.exit(1);
            }
        }
    }


    public static void main(String[] args) {

        int testIteration = 100000;
        for (int i = 0; i < testIteration; i++) {
            int writeThread = 10, readThreads = 3;
            ExecutorService es = Executors.newFixedThreadPool(writeThread + readThreads);
            for (int j = 0; j < writeThread; j++) {
                es.submit(FinalExample::writer);
            }
            for (int j = 0; j < readThreads; j++) {
                es.submit(FinalExample::reader);
            }

            es.shutdown();


            try {
                if (!es.awaitTermination(10, TimeUnit.SECONDS)) {
                    es.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                es.shutdownNow();
            }


        }

    }
}
