package ru.practicum.ewm.handler.action;

import ru.practicum.grpc.stats.event.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto event);
}
