package ru.practicum.ewm.feign.request;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.constant.RequestStatus;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {

        if (cause instanceof FeignException fe) {
            int status = fe.status();
            if (status == 409 || status == 404) {
                return new RequestClient() {
                    @Override
                    public Long countByEventIdAndStatus(Long id, RequestStatus status) {
                        throw fe;
                    }

                    @Override
                    public Map<Long, Long> countConfirmedByEventIds(Set<Long> eventIds) {
                        throw fe;
                    }
                };
            }
        }

        return new RequestClient() {

            @Override
            public Long countByEventIdAndStatus(Long id, RequestStatus status) {
                return 0L;
            }

            @Override
            public Map<Long, Long> countConfirmedByEventIds(Set<Long> eventIds) {
                return eventIds.stream()
                        .collect(Collectors.toMap(id -> id, id -> 0L));
            }
        };
    }
}
