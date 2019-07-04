import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;

public class HelloWorld {
    public static void main(String[] args) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    consume();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "consumer").start();

        produce();

    }


    static String topicname = "testtopic";


    public static void consume() throws Exception {
        Consumer<String, String> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList(topicname));
        String recevied = null;
        while (!"exit".equalsIgnoreCase(recevied)) {
            ConsumerRecords<String, String> rs = consumer.poll(100);
            for (ConsumerRecord<String, String> c : rs) {
                recevied = c.value();
                if (recevied != null) {
                    System.out.printf("recevied topic=%s partition=%s, offset=%s, msg=%s",
                            c.topic(), c.partition(), c.offset(), recevied);
                }
            }
        }

    }

    private static Consumer<String, String> createConsumer() {
        Properties kafkaprops = new Properties();
        kafkaprops.setProperty("bootstrap.servers", "localhost:9092");
        kafkaprops.setProperty("group.id", "consumer_test_group");
        kafkaprops.setProperty("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaprops.setProperty("value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<String, String>(kafkaprops);
    }


    public static void produce() throws Exception {
        Producer<String, String> producer = createProducer();
        String input = null;
        Scanner sc = new Scanner(System.in);
        while (!"exit".equalsIgnoreCase(input)) {
            input = sc.nextLine();
            if (input.length() == 0) {
                System.out.println(">>>> please input sth");
                continue;
            }
            ProducerRecord<String, String> record2Send = new ProducerRecord<>(
                    topicname, "message", input
            );
            RecordMetadata rmd = producer.send(record2Send).get();
            System.out.printf("msg send topic=%s, partition=%s, offset=%d\n",
                    rmd.topic(), rmd.partition(), rmd.offset());
        }
        sc.close();
    }

    private static Producer<String, String> createProducer() {
        Properties kafkaprops = new Properties();
        kafkaprops.setProperty("bootstrap.servers", "localhost:9092");
        kafkaprops.setProperty("key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        kafkaprops.setProperty("value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<String, String>(kafkaprops);
    }
}
