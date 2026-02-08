package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.kafka.KafkaClient;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final KafkaClient kafkaClient;
    private final SimilarityService similarityService;
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    @Value("${topics.user-actions}")
    private String userActionsTopic;
    @Value("${topics.events-similarity}")
    private String eventsSimilarityTopic;

    public void start() {
        log.info("Старт");
        Consumer<String, SpecificRecordBase> consumer = kafkaClient.getConsumer();
        Producer<String, SpecificRecordBase> producer = kafkaClient.getProducer();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            log.info("Подписка на топик: {}", userActionsTopic);
            consumer.subscribe(List.of(userActionsTopic));
            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    continue;
                }
                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    UserActionAvro action = (UserActionAvro) record.value();
                    List<EventSimilarityAvro> similarities = similarityService.updateSimilarity(action);
                    log.info("Получено сообщение: {}", record.value());
                    if (!similarities.isEmpty()) {
                        for (EventSimilarityAvro similarity : similarities) {
                            producer.send(new ProducerRecord<>(eventsSimilarityTopic, similarity));
                            log.info(
                                    "Отправлено сходство в Kafka: events=({}, {}), score={}",
                                    similarity.getEventA(),
                                    similarity.getEventB(),
                                    similarity.getScore()
                            );
                        }
                    }
                }

                consumer.commitAsync();
            }
        } catch (WakeupException ignored) {
            log.info("Получено исключение WakeupException");
        } finally {
            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }
}
