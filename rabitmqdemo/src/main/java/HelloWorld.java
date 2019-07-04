import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

public class HelloWorld {

    public static final String queueName = "hellowordqueue";

    public static void send() throws Exception {
        ConnectionFactory f = new ConnectionFactory();
        f.setHost("127.0.0.1");

        Connection c = f.newConnection();
        Channel ch = c.createChannel();
        ch.queueDeclare(queueName, false, false, false, null);
        String message = "hello world";
        ch.basicPublish("", queueName, null, message.getBytes());
        System.out.println("message sent");
        ch.close();
        c.close();
    }


    public static void recv() throws Exception {
        ConnectionFactory f = new ConnectionFactory();
        f.setHost("127.0.0.1");

        Connection c = f.newConnection();
        Channel ch = c.createChannel();
        ch.queueDeclare(queueName, false, false, false, null);
        Consumer consumer = new DefaultConsumer(ch) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("received - " + new String(body, "UTF-8"));
            }
        };
        ch.basicConsume(queueName, true, consumer);
    }


    public static void main(String[] args) throws Exception{
        send();
        recv();
        send();
        //System.exit(0);
    }
}
