package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.service.EventService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Validated
public class EventController {
    private final EventService service;

    @GetMapping
    public List<EventShortDto> find(
            @ParameterObject @Valid EventsFilter filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable, HttpServletRequest request) {
        return service.findPublicEventsWithFilter(filter, pageable, request);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findById(
            @PathVariable @Positive Long eventId,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
            HttpServletRequest request) throws ConflictException, ConditionsException {
        return service.findPublicEventById(eventId, userId, request);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return service.getRecommendations(userId, size);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) throws ConflictException {
        service.likeEvent(userId, eventId);
    }

    @GetMapping("/category/{catId}/exists")
    public Boolean existsByCategoryId(@PathVariable Long catId) {
        return service.existsByCategoryId(catId);
    }

    @GetMapping("/by-ids")
    public Set<EventShortDto> findAllByIdIn(@RequestParam("ids") Set<Long> ids) throws ConditionsException {
        return service.findAllByIdIn(ids);
    }
}
