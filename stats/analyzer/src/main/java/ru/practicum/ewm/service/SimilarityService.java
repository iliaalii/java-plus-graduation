package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final EventSimilarityRepository repository;

    @Transactional
    public void saveOrUpdate(EventSimilarityAvro value) {
        log.debug("Обработка сходства событий: {} и {}, счет: {}",
                value.getEventA(), value.getEventB(), value.getScore()
        );

        EventSimilarity similarity = EventSimilarity.builder()
                .eventA(value.getEventA())
                .eventB(value.getEventB())
                .score(value.getScore())
                .created(value.getTimestamp())
                .build();

        repository.findByEventAAndEventB(similarity.getEventA(), similarity.getEventB())
                .ifPresent(oldEventSimilarity -> {
                    similarity.setId(oldEventSimilarity.getId());
                    log.debug("Найдено существующее сходство, будет выполнено обновление: id={}",
                            oldEventSimilarity.getId()
                    );
                });

        repository.save(similarity);

        log.info("Сходство событий {} и {} сохранено, счет: {}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore()
        );
    }

    @Transactional(readOnly = true)
    public List<EventSimilarity> findContainsEventsScore(
            Set<Long> eventIds,
            int maxResults
    ) {
        Pageable pageable = PageRequest.of(0, maxResults);

        log.info("Поиск {} пар событий по убыванию score для eventIds (размер={})",
                maxResults, eventIds.size()
        );

        List<EventSimilarity> result = repository.findTopByEventAInOrEventBInOrderByScoreDesc(
                eventIds, eventIds, pageable);

        log.info("Найдено {} пар подходящих событий", result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public List<EventSimilarity> findAllContainsEvent(long eventId) {

        log.info("Поиск всех сходств для события {}", eventId);

        List<EventSimilarity> result = repository.findByEventAOrEventB(eventId);

        log.info("Найдено {} сходств для события: {}", result.size(), eventId);

        return result;
    }
}
