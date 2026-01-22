package ru.practicum.ewm.service.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.dto.compilation.CompilationFullDto;
import ru.practicum.ewm.dto.compilation.CompilationUpdateDto;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.CompilationService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationService compilationService;

    private Compilation compilation;
    private CompilationFullDto compilationDto;
    private CompilationUpdateDto newCompilationDto;
    private CompilationUpdateDto updateRequest;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Test");
        compilation.setPinned(true);
        compilation.setEvents(new HashSet<>());

        compilationDto = new CompilationFullDto();
        compilationDto.setId(1L);
        compilationDto.setTitle("Test");
        compilationDto.setPinned(true);
        compilationDto.setEvents(List.of());

        newCompilationDto = new CompilationUpdateDto();
        newCompilationDto.setTitle("Test");
        newCompilationDto.setPinned(true);
        newCompilationDto.setEvents(List.of());

        updateRequest = new CompilationUpdateDto();
        updateRequest.setTitle("Updated title");
        updateRequest.setPinned(false);
        updateRequest.setEvents(List.of());
    }

    @Test
    void createCompilation() throws ConditionsException {
        when(compilationMapper.toEntity(any(CompilationUpdateDto.class), any()))
                .thenReturn(compilation);
        when(compilationRepository.save(any(Compilation.class)))
                .thenReturn(compilation);
        when(compilationMapper.toFullDto(compilation))
                .thenReturn(compilationDto);

        CompilationFullDto result = compilationService.create(newCompilationDto);

        assertNotNull(result);
        assertEquals("Test", result.getTitle());
        verify(compilationRepository).save(any(Compilation.class));
    }

    @Test
    void deleteCompilation() {
        when(compilationRepository.existsById(1L)).thenReturn(true);

        compilationService.delete(1L);

        verify(compilationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteNotFoundCompilation() {
        when(compilationRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> compilationService.delete(1L));
    }

    @Test
    void getById() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationMapper.toFullDto(compilation)).thenReturn(compilationDto);

        CompilationFullDto result = compilationMapper.toFullDto(compilationService.findById(1L));

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllWhenPinnedNull() {
        Page<Compilation> page = new PageImpl<>(List.of(compilation));
        when(compilationRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(compilationMapper.toFullDto(compilation)).thenReturn(compilationDto);

        List<CompilationFullDto> result = compilationService.find(null, PageRequest.of(0, 10));

        assertEquals(1, result.size());
        verify(compilationRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllWhenPinnedTrue() {
        Page<Compilation> page = new PageImpl<>(List.of(compilation));
        when(compilationRepository.findAllByPinned(eq(true), any(Pageable.class))).thenReturn(page);
        when(compilationMapper.toFullDto(compilation)).thenReturn(compilationDto);

        List<CompilationFullDto> result = compilationService.find(true, PageRequest.of(0, 10));

        assertEquals(1, result.size());
        verify(compilationRepository).findAllByPinned(eq(true), any(Pageable.class));
    }
}