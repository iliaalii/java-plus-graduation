package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.constant.RequestStatus;
import ru.practicum.ewm.service.RequestService;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RequestController {
    private final RequestService service;

    @GetMapping("/{id}/{status}")
    public Long countByEventIdAndStatus(@PathVariable Long id, @PathVariable RequestStatus status) {
        return service.countByEventIdAndStatus(id, status);
    }

    @GetMapping("/count")
    public Map<Long, Long> countConfirmedByEventIds(@RequestParam Set<Long> eventIds) {
        return service.countConfirmedByEventIds(eventIds);
    }
}
