package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {
    private final UserService service;

    @GetMapping
    public List<UserDto> find(
            @RequestParam(required = false) List<Long> ids,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return service.findUsers(ids, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody @Valid UserDto dto) throws ConflictException {
        return service.create(dto);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Positive @PathVariable Long userId) {
        service.delete(userId);
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return service.getUserById(id);
    }

    @GetMapping("/{id}/exists")
    public Boolean existsById(@PathVariable Long id) {
        return service.existsById(id);
    }


    @GetMapping("/{id}/name")
    public String getNameById(@PathVariable Long id) {
        return service.findNameById(id);
    }

    @GetMapping("/by-ids")
    public Map<Long, UserDto> findAllByIds(@RequestParam Set<Long> ids) {
        return service.findAllByIds(ids);
    }
}
