package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {
    private final Map<Long, Map<Long, Double>> eventWeights = new HashMap<>();
    private final Map<Long, Double> eventSummaryWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventMinSummaryWeights = new HashMap<>();

    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction) {
        long userId = userAction.getUserId();
        long eventId = userAction.getEventId();

        Map<Long, Double> userWeights = eventWeights.computeIfAbsent(eventId, e -> new HashMap<>());
        double oldWeight = userWeights.getOrDefault(userId, 0.0);
        double newWeight = getWeightByActionType(userAction.getActionType());

        log.info("Обновление сходства для пользователя {} и события {}: oldWeight={}, newWeight={}",
                userId, eventId, oldWeight, newWeight);

        if (oldWeight >= newWeight) {
            log.debug("Старый вес больше или равен новому весу, обновление не требуется");
            return List.of();
        }

        userWeights.merge(userId, newWeight, Math::max);
        log.debug("Вес пользователя {} для события {} обновлен до {}", userId, eventId, userWeights.get(userId));

        double oldSum = eventSummaryWeights.getOrDefault(eventId, 0.0);
        double newSum = oldSum - oldWeight + newWeight;
        eventSummaryWeights.put(eventId, newSum);
        log.info("Суммарный вес события {} обновлен: oldSum={}, newSum={}", eventId, oldSum, newSum);

        List<EventSimilarityAvro> eventSimilarityAvros = new ArrayList<>();

        for (long otherEventId : eventWeights.keySet()) {
            if (otherEventId == eventId ||
                    eventWeights.get(otherEventId) == null ||
                    !eventWeights.get(otherEventId).containsKey(userId)) {
                continue;
            }
            double newSumMinPairWeight = updateMinWeightSum(eventId, otherEventId, userId, oldWeight, newWeight);
            double similarity = calcSimilarity(eventId, otherEventId, newSumMinPairWeight);
            log.info("Сходство между событиями {} и {}: {}", eventId, otherEventId, similarity);
            eventSimilarityAvros.add(getEventSimilarityAvro(eventId, otherEventId, similarity, userAction.getTimestamp()));
        }

        log.debug("Всего сходств рассчитано для события {}: {}", eventId, eventSimilarityAvros.size());
        return eventSimilarityAvros;
    }

    private void putEventMinSummaryWeights(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        eventMinSummaryWeights
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);

        log.debug("Обновлена minSummaryWeight для пары ({}, {}): {}", first, second, sum);
    }

    private double getEventMinSummaryWeights(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return eventMinSummaryWeights
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private double getWeightByActionType(ActionTypeAvro actionType) {
        double weight = switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
        log.debug("Вес для действия {}: {}", actionType, weight);
        return weight;
    }

    private double updateMinWeightSum(long eventId, long otherEventId, long userId, double oldWeight, double newWeight) {
        double oldWeightOtherEvent = eventWeights.get(otherEventId).get(userId);

        double oldMinPairWeight = Math.min(oldWeight, oldWeightOtherEvent);
        double newMinPairWeight = Math.min(newWeight, oldWeightOtherEvent);

        long firstEventId = Math.min(eventId, otherEventId);
        long secondEventId = Math.max(eventId, otherEventId);

        double oldSumMinPairWeight = getEventMinSummaryWeights(firstEventId, secondEventId);

        if (oldMinPairWeight == newMinPairWeight) {
            log.debug("Мин. вес пары ({}, {}) не изменился", firstEventId, secondEventId);
            return oldSumMinPairWeight;
        }

        double newSumMinPairWeight = oldSumMinPairWeight - oldMinPairWeight + newMinPairWeight;
        putEventMinSummaryWeights(firstEventId, secondEventId, newSumMinPairWeight);

        log.info("Сумма минимальных весов для пары ({}, {}) обновлена: oldSum={}, newSum={}",
                firstEventId, secondEventId, oldSumMinPairWeight, newSumMinPairWeight);

        return newSumMinPairWeight;
    }

    private double calcSimilarity(long eventId, long otherEventId, double newSumMinPairWeight) {
        if (newSumMinPairWeight == 0) return 0;

        double sumEventWeight = eventSummaryWeights.get(eventId);
        double sumOtherEventWeight = eventSummaryWeights.get(otherEventId);
        double similarity = newSumMinPairWeight / (Math.sqrt(sumEventWeight) * Math.sqrt(sumOtherEventWeight));

        log.debug("Вычислено сходство для пары ({}, {}): {}", eventId, otherEventId, similarity);
        return similarity;
    }

    private EventSimilarityAvro getEventSimilarityAvro(long eventId, long otherEventId, double similarity,
                                                       java.time.Instant timestamp) {
        long firstEventId = Math.min(eventId, otherEventId);
        long secondEventId = Math.max(eventId, otherEventId);

        EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                .setEventA(firstEventId)
                .setEventB(secondEventId)
                .setTimestamp(timestamp)
                .setScore(similarity).build();

        log.debug("Сгенерирован EventSimilarityAvro: {}", avro);
        return avro;
    }
}
