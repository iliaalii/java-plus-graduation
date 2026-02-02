package ru.practicum.ewm.feign.user;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.Map;
import java.util.Set;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {

        if (cause instanceof FeignException fe) {
            int status = fe.status();
            if (status == 409 || status == 404) {
                return new UserClient() {
                    @Override
                    public UserDto getUserById(Long id) {
                        throw fe;
                    }

                    @Override
                    public String getNameById(Long id) {
                        throw fe;
                    }

                    @Override
                    public Map<Long, UserDto> findAllByIds(Set<Long> ids) {
                        throw fe;
                    }
                };
            }
        }

        return new UserClient() {

            @Override
            public UserDto getUserById(Long id) {
                throw new NotFoundException("user-service недоступен");
            }

            @Override
            public String getNameById(Long id) {
                return "Unknown";
            }

            @Override
            public Map<Long, UserDto> findAllByIds(Set<Long> ids) {
                throw new NotFoundException("user-service недоступен");
            }
        };
    }
}
