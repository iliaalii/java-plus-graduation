package ru.practicum.ewm.service;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.filter.StatsFilter;
import ru.practicum.ewm.mapper.EndpointHitMapper;
import ru.practicum.ewm.model.EndpointHit;
import ru.practicum.ewm.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointHitServiceTest {

    @Mock
    private EndpointHitMapper mapper;

    @Mock
    private EndpointHitRepository repository;

    @InjectMocks
    private EndpointHitService service;

    private Validator validator;
    private EndpointHitDto validDto;
    private EndpointHit endpointHitEntity;
    private StatsFilter uniqueFilter;
    private StatsFilter nonUniqueFilter;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        validDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test-uri")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        endpointHitEntity = EndpointHit.builder()
                .id(1L)
                .app(validDto.getApp())
                .uri(validDto.getUri())
                .ip(validDto.getIp())
                .created(validDto.getTimestamp())
                .build();

        uniqueFilter = StatsFilter.builder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now())
                .unique(true)
                .build();

        nonUniqueFilter = uniqueFilter.toBuilder()
                .unique(false)
                .build();
    }

    @Test
    void saveEntity() {
        when(mapper.toEntity(validDto)).thenReturn(endpointHitEntity);

        service.createHit(validDto);

        verify(mapper, times(1)).toEntity(validDto);
        verify(repository, times(1)).save(endpointHitEntity);
    }

    @Test
    void callsUniqueMethod() {
        var expected = List.of(new ViewStatsDto());
        when(repository.findStatsByUnique(uniqueFilter)).thenReturn(expected);

        List<ViewStatsDto> result = service.findStats(uniqueFilter);

        assertEquals(expected, result);
        verify(repository, times(1)).findStatsByUnique(uniqueFilter);
        verify(repository, never()).findStatsByNonUnique(any());
    }

    @Test
    void callsNonUniqueMethod() {
        var expected = List.of(new ViewStatsDto());
        when(repository.findStatsByNonUnique(nonUniqueFilter)).thenReturn(expected);

        List<ViewStatsDto> result = service.findStats(nonUniqueFilter);

        assertEquals(expected, result);
        verify(repository, times(1)).findStatsByNonUnique(nonUniqueFilter);
        verify(repository, never()).findStatsByUnique(any());
    }

    @Test
    void returnsEmptyList() {
        when(repository.findStatsByUnique(uniqueFilter)).thenReturn(List.of());

        List<ViewStatsDto> result = service.findStats(uniqueFilter);

        assertTrue(result.isEmpty());
    }

    @Test
    void invalidWhenStartNull() {
        var filter = uniqueFilter.toBuilder().start(null).build();
        var violations = validator.validate(filter);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Время запроса начало")));
    }

    @Test
    void invalidWhenEndNull() {
        var filter = uniqueFilter.toBuilder().end(null).build();
        var violations = validator.validate(filter);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Время запроса окончание")));
    }

    @Test
    void invalidWhenStartAfterEnd() {
        var filter = uniqueFilter.toBuilder()
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now())
                .build();

        var violations = validator.validate(filter);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains(
                "Дата начала не может быть позже даты окончания")));
    }

    @Test
    void validWhenStartBeforeOrEqualEnd() {
        var filter = uniqueFilter.toBuilder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now())
                .build();

        var violations = validator.validate(filter);

        assertTrue(violations.isEmpty());
    }
}
