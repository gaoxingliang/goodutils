package datastructure;

public class TestBoundedBuffer {



    public static void main(String[] args) {
        final BoundedBuffer buf = new BoundedBuffer(10);
        Thread th1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Object o = buf.get();
                        System.out.println("Got one object - " + o + " " + Thread.currentThread().getName());
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th1.setName("Getter1");

        Thread th2 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Object o = buf.get();
                        System.out.println("Got one object - " + o + " " + Thread.currentThread().getName());
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th2.setName("Getter2");

        Thread th3 = new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (true) {
                    try {
                        String o = "string " + i++;
                        buf.put(o);
                        System.out.println("Put one object - " + o + " " + Thread.currentThread().getName());
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                    }

                }
            }
        });
        th3.setName("Putter");

        th1.start();
        th2.start();
        th3.start();
    }
}
