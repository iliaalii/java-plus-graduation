package ru.practicum.ewm.handler.action;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.kafka.KafkaClient;
import ru.practicum.ewm.mapper.UserActionMapper;
import ru.practicum.grpc.stats.event.UserActionProto;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionImpl implements UserActionHandler {
    private final KafkaClient kafkaClient;
    private final UserActionMapper hubEventMapper;

    @Value("${topics.user-action}")
    private String topic;

    @Override
    public void handle(UserActionProto eventProto) {
        kafkaClient.getProducer().send(new ProducerRecord<>(topic, hubEventMapper.toAvro(eventProto)));
        log.info("Действие отправлено в топик: {}", topic);
    }
}
