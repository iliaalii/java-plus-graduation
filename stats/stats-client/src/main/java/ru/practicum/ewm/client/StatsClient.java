package ru.practicum.ewm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.exception.StatsServerUnavailableException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsClient {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    private final String statsServerId = "stats-server";

    @Value("${stats-server.url:http://localhost:9090}")
    private String statsServerUrl;

    private ServiceInstance getInstance() {
        return discoveryClient.getInstances(statsServerId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new StatsServerUnavailableException(
                        "Сервис статистики не найден: " + statsServerId
                ));
    }

    public URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void saveHit(EndpointHitDto hitDto) {
        URI uri;
        try {
            uri = makeUri("/hit");
        } catch (Exception e) {
            log.warn("Eureka недоступна, fallback на statsServerUrl: {}", statsServerUrl);
            uri = URI.create(statsServerUrl + "/hit");
        }

        log.info("Отправка запроса saveHit: url={}, body={}", uri, hitDto);
        restTemplate.postForLocation(uri, hitDto); // POST без тела ответа
        log.info("Hit был сохранен");
    }

    public List<ViewStatsDto> getStats(String start, String end, String[] uris, boolean unique) {
        URI uri;
        try {
            uri = makeUri("/stats");
        } catch (Exception e) {
            log.warn("Eureka недоступна, fallback на statsServerUrl: {}", statsServerUrl);
            uri = URI.create(statsServerUrl + "/stats");
        }

        String url = UriComponentsBuilder.fromUri(uri)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", (Object[]) uris)
                .queryParam("unique", unique)
                .build(false)
                .toUriString();

        String logUrl = url.replace(" ", "%20");
        log.info("Отправка запроса getStats: url={}", logUrl);

        ViewStatsDto[] statsArray = restTemplate.getForObject(url, ViewStatsDto[].class);
        List<ViewStatsDto> stats = Arrays.asList(statsArray != null ? statsArray : new ViewStatsDto[0]);

        log.info("getStats вернул {} записей", stats.size());
        return stats;
    }
}
