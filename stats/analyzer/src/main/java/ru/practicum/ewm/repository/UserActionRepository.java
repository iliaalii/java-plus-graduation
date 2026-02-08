package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByEventIdIn(Set<Long> eventIds);

    List<Long> findDistinctEventIdByUserIdOrderByCreatedDesc(
            Long userId,
            Pageable pageable);

    List<Long> findDistinctEventIdByUserIdAndEventIdIn(Long userId, Set<Long> eventIds);
}
