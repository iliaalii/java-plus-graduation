package ru.practicum.ewm.service;

import com.querydsl.core.BooleanBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.constant.EventState;
import ru.practicum.ewm.constant.EventStateAction;
import ru.practicum.ewm.constant.RequestStatus;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.feign.category.CategoryClient;
import ru.practicum.ewm.feign.comment.CommentClient;
import ru.practicum.ewm.feign.request.RequestClient;
import ru.practicum.ewm.feign.user.UserClient;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;
    private final EventMapper mapper;

    private final UserClient userClient;
    private final LocationService locationService;
    private final RequestClient requestClient;
    private final StatsService statsService;
    private final CommentClient commentClient;
    private final CategoryClient categoryClient;

    @Transactional
    public EventFullDto create(EventNewDto dto, Long userId) throws ConditionsException {
        UserDto initiator = getUserOrThrow(userId);
        CategoryDto category = getCategoryOrThrow(dto.getCategory());

        Location location = locationService.getOrCreateLocation(dto.getLocation());

        Event event = mapper.toEntityFromDto(dto, userId, dto.getCategory(), location);
        event.setCreatedOn(LocalDateTime.now());
        event = repository.save(event);
        log.info("Создано событие с id = {}", event.getId());

        return mapper.eventToFullDto(event, null, null, null, category, initiator);
    }

    @Transactional
    public EventFullDto update(Long userId, Long eventId, EventUpdateDto dto) throws ConditionsException, ConflictException {
        UserDto initiator = getUserOrThrow(userId);

        Event event = getEventOrThrow(eventId, userId);
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменять опубликованное событие");
        }

        Long categoryId = dto.getCategory();
        Location location = (dto.getLocation() == null) ? null : locationService.getOrCreateLocation(dto.getLocation());
        EventState state = dto.getStateAction() == null ?
                null :
                switch (dto.getStateAction()) {
                    case SEND_TO_REVIEW -> EventState.PENDING;
                    case CANCEL_REVIEW -> EventState.CANCELED;
                    case PUBLISH_EVENT, REJECT_EVENT -> null;
                };

        mapper.updateEntityFromDto(event, dto, categoryId, location, state);
        event = repository.save(event);
        log.info("Обновлено событие с id = {}", eventId);

        CategoryDto category = categoryClient.getCategoryById(event.getCategoryId());
        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        return mapper.eventToFullDto(event, calcConfirmedRequests, calcView, getComments(eventId), category, initiator);
    }

    @Transactional
    public EventFullDto updateAdmin(Long eventId, EventUpdateDto dto) throws ConditionsException, ConflictException {
        Event event = getEventOrThrow(eventId);

        EventState currentState = event.getState();
        EventStateAction action = dto.getStateAction();
        LocalDateTime newDate = dto.getEventDate();

        if (action != null) {
            if (action == EventStateAction.PUBLISH_EVENT) {
                if (currentState != EventState.PENDING) {
                    throw new ConflictException("Можно публиковать только события в состоянии PENDING");
                }
            } else if (action == EventStateAction.REJECT_EVENT) {
                if (currentState == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
            }
        }
        if (dto.getEventDate() != null) {
            validateEventDate(newDate, action, currentState, event);
        }

        EventState state = action == null ?
                null :
                switch (action) {
                    case PUBLISH_EVENT -> EventState.PUBLISHED;
                    case REJECT_EVENT -> EventState.CANCELED;
                    case SEND_TO_REVIEW, CANCEL_REVIEW -> null;
                };

        LocalDateTime eventDate = dto.getEventDate() == null ? null : newDate;
        Long categoryId = dto.getCategory();
        Location location = (dto.getLocation() == null) ? null : locationService.getOrCreateLocation(dto.getLocation());

        dto.setEventDate(eventDate);

        mapper.updateEntityFromDto(event, dto, categoryId, location, state);
        event = repository.save(event);
        log.info("Администратор обновил событие с id = {}", eventId);

        UserDto initiator = getUserOrThrow(event.getInitiatorId());
        CategoryDto category = getCategoryOrThrow(event.getCategoryId());
        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        return mapper.eventToFullDto(event, calcConfirmedRequests, calcView, getComments(eventId), category, initiator);

    }

    @Transactional(readOnly = true)
    public EventFullDto findByUserIdAndEventId(Long userId, Long eventId) throws ConditionsException {
        UserDto initiator = getUserOrThrow(userId);

        Event event = getEventOrThrow(eventId, userId);
        CategoryDto category = getCategoryOrThrow(event.getCategoryId());
        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        log.info("Получено событие {} пользователя {}", eventId, userId);
        return mapper.eventToFullDto(event, calcConfirmedRequests, calcView, getComments(eventId), category, initiator);

    }

    @Transactional(readOnly = true)
    public EventFullDto findPublicEventById(Long eventId, HttpServletRequest request) throws ConflictException, ConditionsException {
        Event event = getPublishedEventOrThrow(eventId);
        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        UserDto initiator = getUserOrThrow(event.getInitiatorId());
        CategoryDto category = getCategoryOrThrow(event.getCategoryId());

        statsService.saveHit(
                "category-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );

        log.info("Получено публичное событие {}", eventId);
        return mapper.eventToFullDto(event, calcConfirmedRequests, calcView, getComments(eventId), category, initiator);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> findByUserId(Long userId, Pageable pageable) throws ConditionsException {
        UserDto initiator = getUserOrThrow(userId);

        List<Event> events = repository.findAllByInitiatorId(userId, pageable).getContent();
        if (events.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, CategoryDto> categories = categoryClient.findAllByIds(categoryIds);
        Map<Long, Long> confirmedRequests = requestClient.countConfirmedByEventIds(eventIds);


        return events.stream()
                .map(event -> mapper.eventToShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        statsService.getViewsForEvent(event.getId()),
                        categories.get(event.getCategoryId()),
                        initiator)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> findPublicEventsWithFilter(EventsFilter filter, Pageable pageable, HttpServletRequest request) {
        statsService.saveHit(
                "category-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );
        return findEventsWithFilterInternal(
                filter,
                pageable,
                false,
                (event, viewsMap) -> {
                    String uri = "/events/" + event.getId();
                    Long views = viewsMap.getOrDefault(uri, 0L);

                    CategoryDto categories;
                    try {
                        categories = categoryClient.getCategoryById(event.getCategoryId());
                    } catch (ConditionsException e) {
                        throw new RuntimeException(e);
                    }

                    UserDto initiator = getUserOrThrow(event.getInitiatorId());

                    return mapper.eventToShortDto(event, getConfirmedRequests(event.getId()), views, categories, initiator);
                }
        );
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> findAdminEventsWithFilter(EventsFilter filter, Pageable pageable) {
        return findEventsWithFilterInternal(
                filter,
                pageable,
                true,
                (event, viewsMap) -> {
                    String uri = "/events/" + event.getId();
                    Long views = viewsMap.getOrDefault(uri, 0L);

                    CategoryDto categories;
                    try {
                        categories = categoryClient.getCategoryById(event.getCategoryId());
                    } catch (ConditionsException e) {
                        throw new RuntimeException(e);
                    }

                    UserDto initiator = getUserOrThrow(event.getInitiatorId());

                    return mapper.eventToFullDto(event, getConfirmedRequests(event.getId()), views,
                            getComments(event.getId()), categories, initiator);
                }
        );
    }

    @Transactional(readOnly = true)
    public Set<EventShortDto> findAllByIdIn(Set<Long> ids) throws ConditionsException {
        Set<Event> events = repository.findAllByIdIn(ids);
        if (events.isEmpty()) {
            return Set.of();
        }

        Set<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        Set<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, Long> confirmedRequests = requestClient.countConfirmedByEventIds(eventIds);
        Map<Long, CategoryDto> categories = categoryClient.findAllByIds(categoryIds);
        Map<Long, UserDto> users = userClient.findAllByIds(userIds);


        return events.stream()
                .map(event -> mapper.eventToShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        statsService.getViewsForEvent(event.getId()),
                        categories.get(event.getCategoryId()),
                        users.get(event.getInitiatorId())
                )).collect(Collectors.toSet());
    }

    private <T> List<T> findEventsWithFilterInternal(
            EventsFilter filter,
            Pageable pageable,
            Boolean forAdmin,
            BiFunction<Event, Map<String, Long>, T> mapper) {

        BooleanBuilder predicate = EventPredicateBuilder.buildPredicate(filter, forAdmin);

        if (!forAdmin) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "eventDate")

            );
        }
        Page<Event> eventsPage = repository.findAll(predicate, pageable);

        if (eventsPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = eventsPage.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

        Map<String, Long> viewsUriMap = statsService.getViewsForUris(uris);
        Stream<Event> eventStream = eventsPage.stream();
        List<T> result = eventStream
                .map(e -> mapper.apply(e, viewsUriMap))
                .toList();

        log.info("Найдено {} событий в режиме {}", result.size(), forAdmin ? "ADMIN" : "PUBLIC");
        return result;
    }

    private UserDto getUserOrThrow(Long userId) {
        return userClient.getUserById(userId);
    }

    private CategoryDto getCategoryOrThrow(Long catId) throws ConditionsException {
        return categoryClient.getCategoryById(catId);
    }

    private Event getEventOrThrow(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Event getEventOrThrow(Long eventId, Long userId) throws ConditionsException {
        Event event = getEventOrThrow(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConditionsException("Пользователь не является инициатором события");
        }
        return event;
    }

    private Event getPublishedEventOrThrow(Long eventId) {
        Event event = getEventOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не в состоянии PUBLISHED");
        }
        return event;
    }

    private Long getConfirmedRequests(Long eventId) {
        return requestClient.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private List<CommentDto> getComments(Long eventId) {
        return commentClient.findAllCommentsForEvent(eventId);
    }

    private void validateEventDate(LocalDateTime newDate, EventStateAction action, EventState currentState, Event event) throws ConditionsException {
        if (action == EventStateAction.PUBLISH_EVENT) {
            if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConditionsException("Дата начала должна быть не ранее чем через 1 час при публикации");
            }
        } else if (currentState == EventState.PUBLISHED && event.getPublishedOn() != null) {
            if (newDate.isBefore(event.getPublishedOn().plusHours(1))) {
                throw new ConditionsException("Дата начала должна быть не ранее чем через 1 час после публикации");
            }
        }
    }

    public Boolean existsByCategoryId(Long catId) {
        return repository.existsByCategoryId(catId);
    }
}