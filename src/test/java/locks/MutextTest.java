package locks;

public class MutextTest {
    public static void main(String[] args) throws Exception {
        final Mutex lock = new Mutex();
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                System.out.println("Thread " + Thread.currentThread().getName() + " got lock");
                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread " + Thread.currentThread().getName() + " released lock");
                lock.unlock();
            }
        }, "thread1").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                System.out.println("Thread " + Thread.currentThread().getName() + " got lock");
                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread " + Thread.currentThread().getName() + " released lock");
                lock.unlock();
            }
        }, "thread2").start();

        lock.unlock();

    }
}
