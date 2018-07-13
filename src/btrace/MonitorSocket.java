package btrace;

import com.sun.btrace.AnyType;
import com.sun.btrace.BTraceUtils;
import com.sun.btrace.annotations.*;

import java.util.concurrent.atomic.AtomicInteger;

import static com.sun.btrace.BTraceUtils.*;

/**
 * Monitor the cpu creation stats
 *
 * reference :
 * Monitor using java or aop:
 * https://www.javaspecialists.eu/archive/Issue169.html
 *
 * Monitor using BTrace:
 * https://dzone.com/articles/socket-monitoring-now-using
 */
@BTrace
public class MonitorSocket {

    static AtomicInteger doConnectCalled = BTraceUtils.newAtomicInteger(0);
    static AtomicInteger connectCalled = BTraceUtils.newAtomicInteger(0);


    // connectToAddress(InetAddress address, int port, int timeout)
    @OnMethod(
            clazz="/java\\.net\\.AbstractPlainSocketImpl/",
            method="/.*/"
    )
    public static void anyConnect(@ProbeClassName String pcn, @ProbeMethodName String pmn, AnyType[] args) {
        // print the threadName
        if (BTraceUtils.startsWith(pmn, "connect")) {
            println("connect with thread " + BTraceUtils.currentThread());
            BTraceUtils.printArray(args);

            // this will printout current threadDump
            Threads.jstack();
            BTraceUtils.incrementAndGet(connectCalled);
        } else if (BTraceUtils.startsWith(pmn, "doConnect")) {

            /**
             * doConnect is a subcall of connect method in AbstractPlainSocketImpl
             */
//            println("doConnect " + BTraceUtils.currentThread());
//            BTraceUtils.printArray(args);
//            Threads.jstack();
            incrementAndGet(doConnectCalled);
        }
    }

    /**
     * print the metrics every 10 seconds
     */
    @OnTimer(10000)
    public static void stat() {
        println(BTraceUtils.timestamp("yyyy-MM-dd' 'HH:mm:ss") + " StatSconnect=" + getAndSet(connectCalled, 0) + " doConnect=" + getAndSet(doConnectCalled, 0));
    }

}