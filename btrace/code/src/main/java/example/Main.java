package example;

/**
 * Method 1:
 *
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Thread.sleep(10000);
        System.out.println("Entering");
        method1();
        method2();
    }

    public static void method1() throws InterruptedException {
        Thread.sleep(1900);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Im startrn");
                while (true) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                    System.out.println("running for thr 1");
                }
            }
        });
        th.setName("Thread 1");
        th.start();
    }

    public static void method2() throws InterruptedException {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Im startrn for thread 2");
                while (true) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                    System.out.println("running for thr 2");
                }
            }
        });
        th.setName("Thread 2");
        th.start();
    }
}
