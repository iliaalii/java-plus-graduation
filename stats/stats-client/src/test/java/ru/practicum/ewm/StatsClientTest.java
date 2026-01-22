package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.config.RestClientConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(StatsClient.class)
@Import(RestClientConfig.class)
class StatsClientTest {

    @Autowired
    MockRestServiceServer mockServer;

    @Autowired
    StatsClient client;

    @Value("${stats-server.url}")
    private String statsServerUrl;

    @Test
    void saveHitTest() {
        EndpointHitDto hit = new EndpointHitDto();
        hit.setApp("testApp");
        hit.setUri("/test");
        hit.setIp("127.0.0.1");
        hit.setTimestamp(LocalDateTime.parse(
                "2025-01-01 00:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        ));

        mockServer.expect(requestTo(statsServerUrl + "/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.app").value("testApp"))
                .andExpect(jsonPath("$.uri").value("/test"))
                .andRespond(withSuccess());

        client.saveHit(hit);

        mockServer.verify();
    }

    @Test
    void getStatsTest() {
        String responseJson = "[{\"app\":\"testApp\",\"uri\":\"/test\",\"hits\":5}]";

        mockServer.expect(requestTo(statsServerUrl
                        + "/stats?start=2025-01-01%2000:00:00&end=2025-01-02%2000:00:00&uris=/test&unique=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<ViewStatsDto> stats = client.getStats(
                "2025-01-01 00:00:00",
                "2025-01-02 00:00:00",
                new String[]{"/test"},
                true
        );

        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().getHits()).isEqualTo(5);

        mockServer.verify();
    }
}
