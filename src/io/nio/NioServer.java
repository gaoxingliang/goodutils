package io.nio;

import io.common.ServerInfo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioServer {

    private static class TaskHandler implements Runnable {
        private SocketChannel clientChannel;
        public TaskHandler(SocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }


        @Override
        public void run() {
            ByteBuffer buf = ByteBuffer.allocate(50);
            try {
                boolean flag = true;
                while (flag) {
                    buf.clear();
                    int read = clientChannel.read(buf);
                    String readMessage = new String(buf.array(), 0, read);
                    String writeMessage = "[Echo] " + readMessage + "\n";
                    if ("bye".equalsIgnoreCase(readMessage)) {
                        writeMessage = "[Exit] byebye" + "\n";
                        flag = true;
                    }

                    // 写返回数据
                    buf.clear();
                    buf.put(writeMessage.getBytes());
                    buf.flip(); // 重置缓冲区让其输出
                    clientChannel.write(buf);
                }
                clientChannel.close();

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(10);

        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(ServerInfo.PORT));

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务启动在-" + ServerInfo.PORT);

        while (selector.select() > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    if (socketChannel != null) {
                        // 提交一个任务去处理
                        es.submit(new TaskHandler(socketChannel));
                    }
                }
            }
        }

    }
}
