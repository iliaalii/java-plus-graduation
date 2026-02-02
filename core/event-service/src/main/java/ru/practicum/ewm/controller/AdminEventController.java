package ru.practicum.ewm.controller;

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
import ru.practicum.ewm.dto.event.EventUpdateDto;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController {
    private final EventService service;

    @GetMapping
    public List<EventFullDto> find(
            @ParameterObject @Valid EventsFilter filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return service.findAdminEventsWithFilter(filter, pageable);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(
            @Positive @PathVariable Long eventId,
            @RequestBody @Valid EventUpdateDto dto) throws ConditionsException, ConflictException {
        return service.updateAdmin(eventId, dto);
    }
}
