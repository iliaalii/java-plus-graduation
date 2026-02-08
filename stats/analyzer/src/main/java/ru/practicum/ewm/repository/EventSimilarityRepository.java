package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    Optional<EventSimilarity> findByEventAAndEventB(Long eventIdA, Long eventIdB);

    List<EventSimilarity> findTopByEventAInOrEventBInOrderByScoreDesc(
            Set<Long> eventAIds,
            Set<Long> eventBIds,
            Pageable pageable);

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventA = :eventId OR e.eventB = :eventId")
    List<EventSimilarity> findByEventAOrEventB(@Param("eventId") Long eventId);
}
