package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.repository.UserActionRepository;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionService {
    private final UserActionRepository repository;

    @Transactional
    public void saveOrUpdate(UserActionAvro avroAction) {
        long userId = avroAction.getUserId();
        long eventId = avroAction.getEventId();

        log.debug("Обрабатываем взаимодействие пользователя {}, с событием {}, тип взаимодействия: {}",
                userId, eventId, avroAction.getActionType()
        );

        Optional<UserAction> oldActionOpt =
                repository.findByUserIdAndEventId(userId, eventId);

        if (oldActionOpt.isEmpty()) {
            double rating = getRatingByActionType(avroAction.getActionType());

            UserAction action = UserAction.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .rating(rating)
                    .created(avroAction.getTimestamp())
                    .build();

            repository.save(action);

            log.info("сохранение нового взаимодействия пользователя {}, с событием {}, рейтинг взаимодействия: {}",
                    userId, eventId, rating
            );
        } else {
            UserAction oldAction = oldActionOpt.get();
            double oldRating = oldAction.getRating();
            double newRating = getRatingByActionType(avroAction.getActionType());

            if (newRating >= oldRating) {
                oldAction.setRating(newRating);

                Instant oldTimestamp = oldAction.getCreated();
                Instant newTimestamp = avroAction.getTimestamp();

                if (oldTimestamp == null || oldTimestamp.isBefore(newTimestamp)) {
                    oldAction.setCreated(newTimestamp);
                }

                repository.save(oldAction);

                log.info("обновление взаимодействия пользователя {}, с событием {},старый рейтинг: {}, новый: {}",
                        userId, eventId, oldRating, newRating
                );
            } else {
                log.info("обновление не требуется. новый рейтинг равен или меньше");
            }
        }
    }

    private double getRatingByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    @Transactional(readOnly = true)
    public Set<Long> findLatestEventsForUser(long userId, int maxResult) {
        Pageable pageable = PageRequest.of(0, maxResult);

        log.info("Поиск ближайших {} событий для пользователя {}", maxResult, userId);

        List<Long> eventIds = repository.findDistinctEventIdByUserIdOrderByCreatedDesc(userId, pageable);

        log.info("найдено {} подходящих событий для пользователя {}", eventIds.size(), userId);

        return new HashSet<>(eventIds);
    }

    @Transactional(readOnly = true)
    public Set<Long> findActionsForUser(long userId, Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }

        log.info("Получение взаимодействий пользователя {} ", userId);

        List<Long> result = repository.findDistinctEventIdByUserIdAndEventIdIn(userId, eventIds);

        log.info("найдено {} событий", result.size());

        return new HashSet<>(result);
    }

    @Transactional(readOnly = true)
    public List<UserAction> findActionsForEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserAction> actions =
                repository.findAllByEventIdIn(eventIds).stream().toList();

        log.info("Найдено взаимодействий {}", actions.size());

        return actions;
    }
}
