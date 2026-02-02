package ru.practicum.ewm.feign.event;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;

import java.util.Set;

@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {

    @Override
    public EventClient create(Throwable cause) {
        if (cause instanceof FeignException fe) {
            int status = fe.status();
            if (status == 409 || status == 404) {
                return new EventClient() {
                    @Override
                    public EventFullDto getEventById(Long id) {
                        throw fe;
                    }

                    @Override
                    public Boolean existsByCategoryId(Long catId) {
                        throw fe;
                    }

                    @Override
                    public Set<EventShortDto> findAllByIdIn(Set<Long> ids) {
                        throw fe;
                    }
                };
            }
        }

        return new EventClient() {
            @Override
            public EventFullDto getEventById(Long id) {
                return new EventFullDto();
            }

            @Override
            public Boolean existsByCategoryId(Long catId) {
                return false;
            }

            @Override
            public Set<EventShortDto> findAllByIdIn(Set<Long> ids) {
                return Set.of();
            }
        };
    }
}