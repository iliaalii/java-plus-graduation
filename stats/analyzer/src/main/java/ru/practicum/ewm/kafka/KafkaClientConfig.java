package ru.practicum.ewm.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Configuration
public class KafkaClientConfig {

    @Bean
    public KafkaClient getClient() {
        return new KafkaClient() {
            @Value("${kafka.bootstrap-servers}")
            private String bootstrapServers;
            @Value("${kafka.consumer.key-deserializer}")
            private String keyDeserializer;
            @Value("${kafka.consumer.user-actions-deserializer}")
            private String actionsValueDeserializer;
            @Value("${kafka.consumer.events-similarity-deserializer}")
            private String similarityValueDeserializer;
            @Value("${kafka.consumer.actions-group-id}")
            private String actionsIdGroup;
            @Value("${kafka.consumer.similarity-group-id}")
            private String similarityIdGroup;


            private Consumer<String, EventSimilarityAvro> similarityConsumer;
            private Consumer<String, UserActionAvro> actionConsumer;

            @Override
            public Consumer<String, EventSimilarityAvro> getConsumerSimilarity() {
                if (similarityConsumer == null) {
                    Properties config = new Properties();
                    config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    config.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
                    config.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, similarityValueDeserializer);
                    config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, similarityIdGroup);
                    similarityConsumer = new KafkaConsumer<>(config);
                }
                return similarityConsumer;
            }

            @Override
            public Consumer<String, UserActionAvro> getConsumerUserAction() {
                if (actionConsumer == null) {
                    Properties config = new Properties();
                    config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    config.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
                    config.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, actionsValueDeserializer);
                    config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, actionsIdGroup);
                    actionConsumer = new KafkaConsumer<>(config);
                }
                return actionConsumer;
            }

            @Override
            public void stop() {
                if (similarityConsumer != null) {
                    similarityConsumer.close();
                }
                if (actionConsumer != null) {
                    actionConsumer.close();
                }
            }
        };
    }
}
