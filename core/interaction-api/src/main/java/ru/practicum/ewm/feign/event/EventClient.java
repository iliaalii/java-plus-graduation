package ru.practicum.ewm.feign.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;

import java.util.Set;

@FeignClient(name = "event-service", fallbackFactory = EventClientFallbackFactory.class)
public interface EventClient {
    @GetMapping("/events/{id}")
    EventFullDto getEventById(@PathVariable Long id) throws ConflictException;

    @GetMapping("/events/category/{catId}/exists")
    Boolean existsByCategoryId(@PathVariable Long catId);

    @GetMapping("/events/by-ids")
    Set<EventShortDto> findAllByIdIn(@RequestParam("ids") Set<Long> ids);
}
