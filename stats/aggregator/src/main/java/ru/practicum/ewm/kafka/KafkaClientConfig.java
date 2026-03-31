package ru.practicum.ewm.kafka;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaClientConfig {

    @Bean
    public KafkaClient getClient() {
        return new KafkaClient() {
            @Value("${kafka.bootstrap-servers}")
            private String bootstrapServers;
            @Value("${kafka.producer.key-serializer}")
            private String keySerializer;
            @Value("${kafka.producer.value-serializer}")
            private String valueSerializer;
            @Value("${kafka.consumer.key-deserializer}")
            private String keyDeserializer;
            @Value("${kafka.consumer.value-deserializer}")
            private String valueDeserializer;
            @Value("${kafka.consumer.group-id}")
            private String idGroup;

            private Producer<String, SpecificRecordBase> producer;
            private Consumer<String, SpecificRecordBase> consumer;

            @Override
            public Producer<String, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    initProducer();
                }
                System.out.println("Trying to connect to Kafka at " + bootstrapServers);

                return producer;
            }

            private void initProducer() {
                Properties config = new Properties();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
                producer = new KafkaProducer<>(config);
            }

            @Override
            public void stop() {
                if (producer != null) {
                    producer.close();
                }
            }

            @Override
            public Consumer<String, SpecificRecordBase> getConsumer() {
                if (consumer == null) {
                    initConsumer();
                }
                System.out.println("Trying to connect to Kafka at " + bootstrapServers);

                return consumer;
            }

            private void initConsumer() {
                Properties config = new Properties();
                config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
                config.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
                config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, idGroup);
                consumer = new KafkaConsumer<>(config);
            }
        };
    }
}
