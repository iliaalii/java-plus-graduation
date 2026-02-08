package ru.practicum.ewm.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.grpc.stats.event.ActionTypeProto;
import ru.practicum.grpc.stats.event.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class UserActionMapper {
    public UserActionAvro toAvro(UserActionProto action) {
        log.info("Определение формата ивента");

        Instant timestamp = Instant.ofEpochSecond(action.getTimestamp().getSeconds(), action.getTimestamp().getNanos());

        return UserActionAvro.newBuilder()
                .setUserId(action.getUserId())
                .setEventId(action.getEventId())
                .setTimestamp(timestamp)
                .setActionType(toActionType(action.getActionType()))
                .build();
    }

    public static ActionTypeAvro toActionType(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> null;
        };
    }
}
