package ru.practicum.ewm.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.filter.StatsFilter;
import ru.practicum.ewm.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class EndpointHitRepositoryTest {

    @Autowired
    private EndpointHitRepository repository;

    @Test
    void nonUniqueCountsAllHits() {
        repository.save(new EndpointHit(null, "app1", "/uri1", "1.1.1.1", LocalDateTime.now()));
        repository.save(new EndpointHit(null, "app1", "/uri1", "1.1.1.2", LocalDateTime.now()));

        var filter = StatsFilter.builder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now().plusDays(1))
                .unique(false)
                .build();

        List<ViewStatsDto> result = repository.findStatsByNonUnique(filter);

        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().getHits());
    }

    @Test
    void uniqueCountsDistinctHits() {
        repository.save(new EndpointHit(null, "app1", "/uri1", "1.1.1.1", LocalDateTime.now()));
        repository.save(new EndpointHit(null, "app1", "/uri1", "1.1.1.1", LocalDateTime.now()));

        var filter = StatsFilter.builder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now().plusDays(1))
                .unique(true)
                .build();

        List<ViewStatsDto> result = repository.findStatsByUnique(filter);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getHits());
    }

    @Test
    void filterByUri() {
        repository.save(new EndpointHit(null, "app1", "/uri1", "1.1.1.1", LocalDateTime.now()));
        repository.save(new EndpointHit(null, "app1", "/uri2", "1.1.1.2", LocalDateTime.now()));

        var filter = StatsFilter.builder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now().plusDays(1))
                .uris(List.of("/uri1"))
                .unique(false)
                .build();

        List<ViewStatsDto> result = repository.findStatsByNonUnique(filter);

        assertEquals(1, result.size());
        assertEquals("/uri1", result.getFirst().getUri());
    }

    @Test
    void emptyResult() {
        var filter = StatsFilter.builder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now())
                .unique(false)
                .build();

        List<ViewStatsDto> result = repository.findStatsByNonUnique(filter);

        assertEquals(0, result.size());
    }
}
