package io.aio;

import io.common.ServerInfo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class AioServer {
    public static void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(ServerInfo.PORT));
        serverSocketChannel.accept(null, new AcceptFinishHandler());
        System.out.println("Server 启动了 - " + ServerInfo.PORT);
        CountDownLatch running = new CountDownLatch(1);
        running.await();
    }

    static class ReadFinishHandler implements CompletionHandler<Integer, AsynchronousSocketChannel> {
        private ByteBuffer buf;
        public ReadFinishHandler(ByteBuffer buf) {
            this.buf = buf;
        }
        @Override
        public void completed(Integer result, AsynchronousSocketChannel attachment) {
            System.out.println("Read resp from client is:");
            String readStr = new String(buf.array(), 0, result);
            System.out.println(readStr);
            ByteBuffer buf = ByteBuffer.allocate(512);
            buf.put(("[ECHO] " + readStr.trim() + "\n").getBytes());
            buf.flip();
            attachment.write(buf, attachment, new WriteFinishHandler());
        }

        @Override
        public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
            System.out.println("Error when reading");
            exc.printStackTrace();
        }
    }

    static class WriteFinishHandler implements CompletionHandler<Integer, AsynchronousSocketChannel> {

        @Override
        public void completed(Integer result, AsynchronousSocketChannel attachment) {
            // 数据已经写完了 等待下一次的数据读取
            System.out.println("write has finished and write bytes - " + result);
            ByteBuffer buf = ByteBuffer.allocate(512);
            attachment.read(buf, attachment, new ReadFinishHandler(buf));
        }

        @Override
        public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
            System.out.println("Error during writing");
            exc.printStackTrace();
        }
    }

    static class AcceptFinishHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel socketChannel, Object attachment) {
            // 因为我们是服务端 那么此时我们应该要准备读取数据了
            ByteBuffer buf = ByteBuffer.allocate(512);
            socketChannel.read(buf, socketChannel, new ReadFinishHandler(buf));
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("Fail to accept");
            exc.printStackTrace();
        }
    }
}

