package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;


    @Transactional
    public UserDto create(UserDto userDto) throws ConflictException {
        log.info("Создать пользователя. email: {}", userDto.getEmail());
        if (isEmailExistsAnotherUser(userDto)) {
            throw new ConflictException("Адрес электронной почты уже используется");
        }
        User user = repository.save(mapper.toEntity(userDto));
        log.info("Создание пользователя OK, id = {}", user.getId());
        return mapper.toDto(user);
    }

    @Transactional
    public void delete(Long userId) {
        if (!userIsExist(userId)) {
            throw new NotFoundException("Удаляемая запись не найдена");
        }
        repository.deleteById(userId);
        log.info("Удален пользователь id = {}", userId);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findUsers(List<Long> ids, Pageable pageable) {
        var result = (CollectionUtils.isEmpty(ids))
                ? repository.findAll(pageable).stream().toList()
                : repository.findAllById(ids);
        return result.stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Boolean isEmailExistsAnotherUser(UserDto userDto) {
        return Optional.ofNullable(userDto.getId())
                .map(id -> repository.existsByEmailAndIdNot(userDto.getEmail(), id))
                .orElseGet(() -> repository.existsByEmail(userDto.getEmail()));
    }

    @Transactional(readOnly = true)
    public Boolean userIsExist(Long userId) {
        return repository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public String findNameById(Long id) {
        return repository.findNameById(id);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return mapper.toDto(repository.findById(id).orElseThrow(() ->
                new NotFoundException("Пользователь с id=" + id + " не найден")
        ));
    }

    @Transactional(readOnly = true)
    public Map<Long, UserDto> findAllByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<User> users = repository.findAllByIdIn(ids);

        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        mapper::toDto
                ));
    }
}
