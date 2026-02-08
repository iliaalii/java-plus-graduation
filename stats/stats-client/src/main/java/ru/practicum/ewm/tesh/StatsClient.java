package ru.practicum.ewm.tesh;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.event.ActionTypeProto;
import ru.practicum.grpc.stats.event.UserActionControllerGrpc;
import ru.practicum.grpc.stats.event.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class StatsClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionStub;

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordView(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
        log.info("Получен запрос просмотра");
    }

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordRegister(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordLike(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendAction(Long userId, Long eventId, ActionTypeProto actionType) {
        try {
            UserActionProto userAction = UserActionMapper.toProto(userId, eventId, actionType, Instant.now());
            userActionStub.collectUserAction(userAction);
            log.info("Действие пользователя успешно отправлено: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);
        } catch (StatusRuntimeException e) {
            log.error("Не удалось отправить действие пользователя. Статус: {}, сообщение: {}" +
                            "TEST: {}, {}, {}",
                    e.getStatus(), e.getMessage(),userId, eventId, actionType, e);
            throw e;
        } catch (Exception e) {
            log.error("Не удалось отправить действие пользователя. Исключение: {}, сообщение: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке действия пользователя", e);
        }
    }
}
