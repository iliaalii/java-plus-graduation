package ru.practicum.ewm.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.ActionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionProcessor implements Runnable {
    private final KafkaClient kafkaClient;
    private final ActionService service;

    @Value("${topics.user-actions}")
    private String topic;

    @Override
    public void run() {
        log.info("Старт ActionProcessor для топика '{}'", topic);
        Consumer<String, UserActionAvro> consumer = kafkaClient.getConsumerUserAction();
        consumer.subscribe(List.of(topic));
        try {
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofSeconds(5));
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    log.info("Получено сообщение {}", record.value());
                    service.saveOrUpdate(record.value());
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка при обработке событий Kafka", e);
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
