package ru.practicum.ewm.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface KafkaClient {

    Consumer<String, EventSimilarityAvro> getConsumerSimilarity();

    Consumer<String, UserActionAvro> getConsumerUserAction();

    void stop();
}
