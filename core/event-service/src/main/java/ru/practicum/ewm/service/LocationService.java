package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventLocationDto;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.repository.LocationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository repository;
    private final LocationMapper mapper;

    @Transactional
    public Location getOrCreateLocation(EventLocationDto locationDto) {
        if (locationDto == null) {
            throw new IllegalArgumentException("LocationDto не может быть null");
        }
        log.info("Ищем или создаем новую локацию: {}", locationDto);

        return repository.findFirstByLatAndLon(locationDto.getLat(), locationDto.getLon())
                .orElseGet(() -> {
                    log.info("Локация не найдена, создаем новую");
                    Location newLocation = mapper.toEntity(locationDto);
                    return repository.save(newLocation);
                });
    }
}
