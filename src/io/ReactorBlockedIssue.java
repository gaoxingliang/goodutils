package io;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.AbstractIOReactor;
import org.apache.http.impl.nio.reactor.AbstractMultiworkerIOReactor;
import org.apache.http.impl.nio.reactor.BaseIOReactor;
import org.apache.http.impl.nio.reactor.ChannelEntry;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最终会有6个IO相关的线程<br/>
 * 每次new一个CloseableHttpAsyncClient对象都会生成一个ReactorThread 来负责分发事件 [我们暂且叫做MainReactor]<br/>
 * <pre>
 *         public CloseableHttpAsyncClientBase(  // 的构造函数...
 *         this.connmgr = connmgr;
 *         if (threadFactory != null && handler != null) {
 *             this.reactorThread = threadFactory.newThread(new Runnable() {
 *                 public void run() {
 *                         final IOEventDispatch ioEventDispatch = new InternalIODispatch(handler);
 *                         connmgr.execute(ioEventDispatch);
 *                     }
 *                 }
 *             });
 * </pre>
 * <pre>
 *      org.apache.http.impl.nio.reactor.AbstractMultiworkerIOReactor#execute(org.apache.http.nio.reactor.IOEventDispatch)
 * </pre>
 * <p>
 * AbstractMultiworkerIOReactor#execute步骤 在MainReactor 中执行:<br/>
 * <pre>
 *     伪代码:
 *      1. 启动worker dispatch线程<br/>
 *             for (int i = 0; i < this.workerCount; i++) {
 *                 final BaseIOReactor dispatcher = this.dispatchers[i];
 *                 this.workers[i] = new Worker(dispatcher, eventDispatch);
 *                 this.threads[i] = this.threadFactory.newThread(this.workers[i]);
 *             }
 *      2. 执行一个while true死循环:<br/>
 *         2.1 执行selector.select(timeout) 然后得到一个int值 count 标记selector上ready的事件个数.<br/>
 *         2.2 先处理, 需要连接到远端的请求的集合中的请求 org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor#processSessionRequests()
 *              如果有需要连接到远端的请求(从requestQueue中不停的poll), 就直接连接,<br/>
 *                  如果连接成功, 则生成ChannelEntry并按照RoundRobin的方式分配到对应的Dispatcher线程中作为后续处理 (放到dispatcher的newChannels集合)<br/>
 *                  如果有异常, 检查并设置该请求对应的异常调用回调.<br/>
 *                  如果正在连接非阻塞模式, 则注册到selector上.<br/>
 *         2.3 处理2.1中select出来的key的请求. 如果是Connectable, 就生成对应的ChannelEntry并分发到对应的Dispatcher.<br/>
 *
 * </pre>
 * 所以MainReactor只会处理Connect事件, Connect完成后就分发给Dispatcher线程执行真正的IO操作了.<br/>
 * <p>
 * 然后还会生成5个真正的执行线程负责执行真正的IO操作比如读取, 写入.<br/>
 * 单个Dispatcher按照如下流程执行 (每个Dispatcher有自己的Selector(绑定了一个BaseIOReactor), 和主Selector不是同一个.)<br/>
 * <pre>
 *      跟MainReactor很类似, 也是一个死循环.
 *      1. 执行selector.select事件.
 *      2. 如果有可用的selectionkey事件, 那么会处理.
 *            但是处理该channel上的所有IO事件(读 写 连接... 这个过程会阻塞 并调用callback的处理函数 如果消息完全读取的话)
 *            如下:org.apache.http.impl.nio.reactor.BaseIOReactor#readable(java.nio.channels.SelectionKey)
 *       protected void readable(final SelectionKey key) {
 *         final IOSession session = getSession(key);
 *         try {
 *             // Try to gently feed more data to the event dispatcher
 *             // if the session input buffer has not been fully exhausted
 *             // (the choice of 5 iterations is purely arbitrary)
 *             for (int i = 0; i < 5; i++) {
 *                 this.eventDispatch.inputReady(session);
 *                 if (!session.hasBufferedInput()
 *                         || (session.getEventMask() & SelectionKey.OP_READ) == 0) {
 *                     break;
 *                 }
 *             }
 *      3. 处理newChannels上的集合 (前面MainReactor放到这个集合里面的.).  把他们移除newChannels并register到selector上.<br/>
 * </pre>
 * 所以  我们可以想象:<br/>
 * 如果我们的某个Callback需要执行很久很久, 那么 Dispatcher线程会被block住, newChannels里面待处理的ChannelEntry会越来越多.<br/>
 * <p>
 * 而这个类就展示了这种case.  你会看到 有些请求还是可以返回成功, 但是有些就是不行, 而且newChannels集合在变大.<br/>
 */
public class ReactorBlockedIssue {
    public static void main(String[] args) throws Exception {

        IOReactorConfig config = IOReactorConfig.copy(IOReactorConfig.DEFAULT)
                .setConnectTimeout(30000)
                .setSoTimeout(30000)
                .setIoThreadCount(5)
                .build();

        CloseableHttpAsyncClient c = HttpAsyncClientBuilder.create()
                .setThreadFactory(new ThreadFactory() {

                    AtomicInteger id = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread th = new Thread(r);
                        th.setName("RectorThread-" + id.incrementAndGet());
                        return th;
                    }
                })
                .setDefaultIOReactorConfig(config).build();
        c.start();

        HttpGet get = new HttpGet("https://qq.com");
        c.execute(get, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                System.out.println( Thread.currentThread() + "Got response - " + result);
                System.out.println("我们会阻塞:" + Thread.currentThread());
                //睡眠很久试试
                try {
                    Thread.sleep(1000000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void failed(Exception ex) {
                System.out.println("Got ex - " + ex);
            }

            @Override
            public void cancelled() {

            }
        });
        // 我们每隔一段时间触发一个新的请求
        // 多加一点website来更快触发.
        String [] tests = new String[]{"http://www.hao123.com/", "http://qq.com", "http://bing.com", "http://taobao.com", "http://jd.com", "http://toutiao.com"};

        for (String t : tests) {
            final String test = t;
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    HttpGet get = new HttpGet(test + "?t=" + System.currentTimeMillis());
                    c.execute(get, new FutureCallback<HttpResponse>() {

                        @Override
                        public void completed(HttpResponse result) {
                            System.out.println("Got internal response - " + result);
                        }

                        @Override
                        public void failed(Exception ex) {
                            System.out.println("Got ex - " + ex);
                        }

                        @Override
                        public void cancelled() {

                        }
                    });
                }
            }, 1, 5, TimeUnit.SECONDS);
        }

        // 生成一个线程来利用反射获取那个集合.
        printChannelsSize(c);
    }

    private static void printChannelsSize(CloseableHttpAsyncClient c) {
        // InternalHttpAsyncClient
        String className = "org.apache.http.impl.nio.client.InternalHttpAsyncClient";
        try {
            Class clientClz = Class.forName(className);
            Field f = clientClz.getDeclaredField("connmgr");
            f.setAccessible(true);
            PoolingNHttpClientConnectionManager connmgr = (PoolingNHttpClientConnectionManager) f.get(c);

            Field connectReactorField = PoolingNHttpClientConnectionManager.class.getDeclaredField("ioreactor");
            connectReactorField.setAccessible(true);
            AbstractMultiworkerIOReactor reactor = (AbstractMultiworkerIOReactor) connectReactorField.get(connmgr);
            Field dispatchersField = AbstractMultiworkerIOReactor.class.getDeclaredField("dispatchers");
            dispatchersField.setAccessible(true);
            BaseIOReactor[] dispatchers = (BaseIOReactor[])dispatchersField.get(reactor);

            Field threadsField = AbstractMultiworkerIOReactor.class.getDeclaredField("threads");
            threadsField.setAccessible(true);
            Thread[] threads = (Thread[])threadsField.get(reactor);
            for (Thread t : threads) {
                System.out.println("\t\tThe dispatcher thread is - " + t);
            }


            Field channelsField = AbstractIOReactor.class.getDeclaredField("newChannels");
            channelsField.setAccessible(true);

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    // 因为连接会复用 并不会重新建socket
                    // 所以这里每次都关掉1s前没有使用的socket.
                    // 而我们使用socket的频率是5s一次 理论上来说都会重建.
                    connmgr.closeIdleConnections(1, TimeUnit.SECONDS);
                    for (BaseIOReactor d : dispatchers) {
                        Queue<ChannelEntry> newChannels = null;
                        try {
                            newChannels = (Queue<ChannelEntry>) channelsField.get(d);
                        }
                        catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        System.out.println("\t\t当前rector还有多少个连接等待处理:" + newChannels.size());
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);

        }
        catch (Throwable e) {
            e.printStackTrace();
        }

    }
}
