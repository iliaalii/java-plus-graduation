package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.service.RequestService;

import javax.naming.ServiceUnavailableException;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserRequestController {
    private final RequestService service;

    @GetMapping
    public List<ParticipationRequestDto> findByUser(
            @Positive @PathVariable Long userId) throws ServiceUnavailableException {
        return service.getRequestsByUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(
            @Positive @PathVariable Long userId,
            @RequestParam Long eventId) throws ConflictException {
        return service.create(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancel(
            @Positive @PathVariable Long userId,
            @Positive @PathVariable Long requestId) throws ConditionsException {
        return service.cancelRequest(userId, requestId);
    }
}
