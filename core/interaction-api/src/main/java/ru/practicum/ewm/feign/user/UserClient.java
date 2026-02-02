package ru.practicum.ewm.feign.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @GetMapping("/admin/users/{id}")
    UserDto getUserById(@PathVariable Long id);

    @GetMapping("/admin/users/{id}/name")
    String getNameById(@PathVariable("id") Long id);

    @GetMapping("/admin/users/by-ids")
    Map<Long, UserDto> findAllByIds(@RequestParam Set<Long> ids);
}
