package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.constant.RequestStatus;
import ru.practicum.ewm.model.Request;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findAllByIdInAndStatus(List<Long> ids, RequestStatus status);

    List<Request> findByEventIdInAndStatus(Set<Long> eventIds, RequestStatus status);
}
