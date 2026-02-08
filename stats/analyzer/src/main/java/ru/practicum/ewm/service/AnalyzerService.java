package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzerService {

    private final ActionService actionService;
    private final SimilarityService similarityService;

    public Iterable<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        log.info("Запрос рекомендаций для пользователя: userId={}, maxResults={}",
                request.getUserId(), request.getMaxResults()
        );

        Set<Long> actionIds = actionService.findLatestEventsForUser(request.getUserId(), request.getMaxResults());

        log.debug("Последние события пользователя userId={}: {}", request.getUserId(), actionIds);

        if (actionIds.isEmpty()) {
            return List.of();
        }

        List<EventSimilarity> similarities = similarityService.findContainsEventsScore(
                actionIds, request.getMaxResults());

        log.debug("Найдено {} записей сходства для пользователя userId={}", similarities.size(), request.getUserId());

        Map<Long, Double> eventIds = similarities.stream()
                .collect(Collectors.toMap(
                        o -> actionIds.contains(o.getEventA()) ? o.getEventB() : o.getEventA(),
                        EventSimilarity::getScore,
                        Double::max
                ));

        log.info("Сформировано {} рекомендаций для пользователя userId={}", eventIds.size(), request.getUserId());

        return eventIds.entrySet().stream()
                .map(o -> RecommendedEventProto.newBuilder()
                        .setEventId(o.getKey())
                        .setScore(o.getValue())
                        .build())
                .toList();
    }

    public Iterable<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.info("Запрос похожих событий: eventId={}, userId={}, maxResults={}",
                request.getEventId(), request.getUserId(), request.getMaxResults()
        );

        List<EventSimilarity> similarPair = similarityService.findAllContainsEvent(request.getEventId());

        log.debug("Найдено {} пар сходства для eventId={}", similarPair.size(), request.getEventId());

        Set<Long> ids = similarPair.stream()
                .map(EventSimilarity::getEventA)
                .collect(Collectors.toSet());

        Set<Long> otherIds = similarPair.stream()
                .map(EventSimilarity::getEventB)
                .collect(Collectors.toSet());

        ids.addAll(otherIds);

        Set<Long> userEventIds = actionService.findActionsForUser(request.getUserId(), ids);

        log.debug("Пользователь userId={} уже взаимодействовал с событиями: {}", request.getUserId(), userEventIds);

        int beforeFilter = similarPair.size();

        similarPair.removeIf(o ->
                userEventIds.contains(o.getEventA())
                        && userEventIds.contains(o.getEventB())
        );

        log.debug("Отфильтровано {} пар сходства (уже просмотренные пользователем)", beforeFilter - similarPair.size());

        return similarPair.stream()
                .sorted(Comparator.comparing(
                        EventSimilarity::getScore,
                        Comparator.reverseOrder()
                ))
                .limit(request.getMaxResults())
                .map(o -> {
                    long eventId = Objects.equals(o.getEventA(), request.getEventId())
                            ? o.getEventB()
                            : o.getEventA();
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(o.getScore())
                            .build();
                })
                .toList();
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("Запрос количества взаимодействий для {} событий", request.getEventIdList().size());

        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        List<UserAction> userActions = actionService.findActionsForEvents(eventIds);

        log.debug("Найдено {} пользовательских действий", userActions.size());

        Map<Long, Double> actionMap = userActions.stream()
                .collect(Collectors.groupingBy(
                        UserAction::getEventId,
                        Collectors.summingDouble(UserAction::getRating)
                ));

        log.info("Подсчитано взаимодействий для {} событий", actionMap.size());

        return actionMap.entrySet().stream()
                .map(o -> RecommendedEventProto.newBuilder()
                        .setEventId(o.getKey())
                        .setScore(o.getValue())
                        .build())
                .toList();
    }
}
