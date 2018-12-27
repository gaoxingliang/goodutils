package cpu;

import com.sun.management.OperatingSystemMXBean;
import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a example code about how to get the process cpu and thread cpu in java internal app
 */
public class CpuTest {

    public static void main(String[] args) {
        final AtomicInteger seq = new AtomicInteger(0);

        ScheduledExecutorService es = Executors.newScheduledThreadPool(20, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setName("consumingthreads-" + seq.incrementAndGet());
                return th;
            }
        });

        for (int i = 0; i < 200; i++) {
            es.scheduleAtFixedRate(new ConsumingCpuTask(), 0, 10, TimeUnit.MILLISECONDS);
        }

        // not terminate the es


        // another thread to print host cpu
        ScheduledExecutorService printer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setName("printer");
                return th;
            }
        });
        // print every 10 seconds
        printer.scheduleAtFixedRate(new PrintCurrentProcessCpuTask(), 0, 10, TimeUnit.SECONDS);
        printer.scheduleAtFixedRate(new PrintThreadCpuTask(), 0, 10, TimeUnit.SECONDS);
    }

    static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
    // notice here is com.sun.management.OperatingSystemMXBean and it's not java.lang.management.OperatingSystemMXBean
    static final OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    /**
     * get process cpu in nanoseconds
     */
    static double getProcessCpuTime() {
        return bean.getProcessCpuTime();
    }


    /**
     * A task to simulate consuming cpu
     */
    static class ConsumingCpuTask implements Runnable {

        @Override
        public void run() {
            AtomicInteger integer = new AtomicInteger(0);
            for (int i = 0; i < 10000; i++) {
                integer.incrementAndGet();
            }
        }
    }

    // it's com.sun.management.ThreadMXBean
    static ThreadMXBean threadMXBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();

    static class PrintThreadCpuTask implements Runnable {
        // not consider thread safe here
        Map<Long, Long> threadId2CpuTime = null;

        Map<Long, String> threadId2Name = null;
        private long collectTime = 0;

        @Override
        public void run() {
            if (threadId2CpuTime == null) {
                threadId2CpuTime = new HashMap<>();
                threadId2Name = new HashMap<>();
                long threads[] = threadMXBean.getAllThreadIds();
                long cpuTimes[] = threadMXBean.getThreadCpuTime(threads);
                for (int i = 0; i < threads.length; i++) {
                    threadId2CpuTime.put(threads[i], cpuTimes[i]);
                    // get the thread name
                    // maybe null, if not exists any more
                    ThreadInfo info = threadMXBean.getThreadInfo(threads[i]);
                    if (info != null) {
                        threadId2Name.put(threads[i], info.getThreadName());
                    }
                }
                collectTime = System.currentTimeMillis();
            }
            else {
                long threads[] = threadMXBean.getAllThreadIds();
                long cpuTimes[] = threadMXBean.getThreadCpuTime(threads);
                Map<Long, Long> newthreadId2CpuTime = new HashMap<>();
                for (int i = 0; i < threads.length; i++) {
                    newthreadId2CpuTime.put(threads[i], cpuTimes[i]);
                }

                long newCollectTime = System.currentTimeMillis();
                threadId2CpuTime.entrySet().forEach(en -> {
                    long threadId = en.getKey();
                    Long time = en.getValue();
                    Long newTime = newthreadId2CpuTime.get(threadId);
                    if (newTime != null) {
                        double cpu = (newTime - time) * 1.0d / (newCollectTime - collectTime) / 1000000L / PROCESSOR_COUNT;
                        System.out.println(String.format("\t\tThread %s cpu is: %.2f %%", threadId2Name.get(threadId), cpu * 100));
                    }
                    threadId2CpuTime.put(threadId, newTime);

                });
            }
        }
    }

    static class PrintCurrentProcessCpuTask implements Runnable {

        double cpuTime = 0;
        long collectTime = 0;

        @Override
        public void run() {
            if (cpuTime == 0) {
                cpuTime = getProcessCpuTime();
                collectTime = System.currentTimeMillis();
            }
            else {
                double newCpuTime = getProcessCpuTime();
                long newCollectTime = System.currentTimeMillis();
                double cpu = (newCpuTime - cpuTime) * 1.0d / (newCollectTime - collectTime) / 1000_000 / PROCESSOR_COUNT;
                cpuTime = newCpuTime;
                collectTime = newCollectTime;
                System.out.println(String.format("Process cpu is: %.2f %%", cpu * 100));
            }
        }
    }
}
