package datastructure;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实现一个有界阻塞队列
 */
public class BoundedBuffer {

    private final Object [] array;
    private volatile int nextPutIndex = 0, cursize = 0;
    private final int size;
    private ReentrantLock lock = new ReentrantLock(true);
    private Condition notEmpty = lock.newCondition();
    private Condition notFull = lock.newCondition();

    public BoundedBuffer(int size) {
        array = new Object[size];
        this.size = size;
    }


    public Object get() throws InterruptedException {
        lock.lock();
        try {
            while (cursize == 0) {
                notEmpty.await();
            }
            int index = nextPutIndex > 0 ? nextPutIndex - 1: size -1;
            Object o = array[index];
            nextPutIndex = index;
            cursize --;
            notFull.signalAll();
            return o;
        }
        finally {
            lock.unlock();
        }
    }

    public void put(Object o) throws InterruptedException{
        lock.lock();

        try {
            while (cursize == size) {
                notFull.await();
            }
            array[nextPutIndex] = o;
            nextPutIndex = nextPutIndex == size ? 0 : nextPutIndex + 1;
            cursize++;
            notEmpty.signalAll();
        }
        finally {
            lock.unlock();
        }
    }

}
