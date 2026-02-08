package ru.practicum.ewm.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProcessor implements Runnable {
    private final KafkaClient kafkaClient;
    private final SimilarityService service;

    @Value("${topics.events-similarity}")
    private String topic;

    @Override
    public void run() {
        log.info("Старт SimilarityProcessor для топика '{}'", topic);
        Consumer<String, EventSimilarityAvro> consumer = kafkaClient.getConsumerSimilarity();
        consumer.subscribe(List.of(topic));

        try {
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(Duration.ofSeconds(5));
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    log.info("Получено сообщение {}", record.value());
                    service.saveOrUpdate(record.value());
                    log.info("Схожесть обработана.");
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка обработки сообщений", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception ignored) {
            }
            consumer.close();
            log.info("Консьюмер закрыт");
        }
    }
}
