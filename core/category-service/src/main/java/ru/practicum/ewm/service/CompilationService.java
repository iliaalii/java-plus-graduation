package ru.practicum.ewm.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.interfaceValidation.CreateValidation;
import ru.practicum.ewm.core.interfaceValidation.UpdateValidation;
import ru.practicum.ewm.dto.compilation.CompilationFullDto;
import ru.practicum.ewm.dto.compilation.CompilationUpdateDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.feign.event.EventClient;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.repository.CompilationRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class CompilationService {

    private final CompilationRepository repository;
    private final CompilationMapper mapper;
    private final EventClient eventClient;

    @Transactional
    @Validated(CreateValidation.class)
    public CompilationFullDto create(@Valid CompilationUpdateDto dto) throws ConditionsException {
        Set<Long> eventIds = getUniqueEventIds(dto.getEvents());

        Compilation compilation = repository.save(mapper.toEntity(dto, eventIds));
        Set<EventShortDto> event = eventClient.findAllByIdIn(compilation.getEvents());
        log.info("Создана подборка, id = {}", compilation.getId());
        return mapper.toFullDto(compilation, event);
    }

    @Transactional
    public void delete(Long compId) {
        if (!compilationIsExist(compId)) {
            throw new NotFoundException("Подборка с id " + compId + " не найдена");
        }
        repository.deleteById(compId);
        log.info("Удалена подборка id = {}", compId);
    }


    @Transactional
    @Validated(UpdateValidation.class)
    public CompilationFullDto update(Long compId, @Valid CompilationUpdateDto dto) throws ConditionsException {
        var compilation = findById(compId);

        Set<Long> events = getUniqueEventIds(dto.getEvents());
        compilation = mapper.toEntityGeneral(compilation, dto, events);
        Set<EventShortDto> event = eventClient.findAllByIdIn(compilation.getEvents());
        log.info("Обновлена подборка id = {}", compId);
        return mapper.toFullDto(compilation, event);
    }

    @Transactional(readOnly = true)
    public Compilation findById(Long id) throws NotFoundException {
        return repository.findById(id == null ? 0L : id)
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
    }

    @Transactional(readOnly = true)
    public List<CompilationFullDto> find(Boolean pinned, Pageable pageable) {
        var page = (pinned != null)
                ? repository.findAllByPinned(pinned, pageable)
                : repository.findAll(pageable);

        return page.getContent()
                .stream()
                .map(comp -> mapper.toFullDto(comp, eventClient.findAllByIdIn(comp.getEvents())))
                .toList();
    }

    private Set<Long> getUniqueEventIds(List<Long> eventIds) throws ConditionsException {
        if (eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> unique = new HashSet<>(eventIds);
        if (unique.size() != eventIds.size()) {
            throw new ConditionsException("Список событий содержит дубликаты");
        }

        return unique;
    }

    @Transactional(readOnly = true)
    public Boolean compilationIsExist(Long id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public CompilationFullDto getEntityFool(Long compId) {
        Compilation comp = findById(compId);
        return mapper.toFullDto(comp, eventClient.findAllByIdIn(comp.getEvents()));
    }
}
