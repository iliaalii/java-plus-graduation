package ru.practicum.ewm.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.NotFoundException;
import ru.practicum.ewm.core.interfaceValidation.CreateValidation;
import ru.practicum.ewm.core.interfaceValidation.UpdateValidation;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.CommentUpdateDto;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Transactional(readOnly = true)
    public CommentDto findById(Long id) throws ConditionsException {
        return commentMapper.toDto(getCommentOrThrow(id));
    }

    @Transactional
    @Validated(CreateValidation.class)
    public CommentDto create(@Valid CommentUpdateDto entity, Long userId) throws ConditionsException {
        var comment = Comment.builder()
                .author(getUserOrThrow(userId))
                .event(getEventOrThrow(entity.getEventId()))
                .text(entity.getText())
                .deleted(false)
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();
        log.info("Создание нового комментария к событию с id={} пользователем с id={}", entity.getEventId(), userId);
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    @Validated(UpdateValidation.class)
    public CommentDto update(@Valid CommentUpdateDto dto, Long commentId, Long userId) throws ConditionsException {
        var comment = getCommentOrThrow(commentId);
        if (!dto.getIsAdmin() && !isAuthorComment(comment.getAuthor(), userId)) {
            throw new ConditionsException("Вы не можете редактировать данный комментарий");
        }
        comment = commentMapper.mapEntityFromDto(comment, dto);
        log.info("Обновление комментария к событию с id={} пользователем с id {}", comment.getEvent().getId(), userId);
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long commentId, Long userId) throws ConditionsException {
        CommentUpdateDto dto = CommentUpdateDto.builder().deleted(true).build();
        update(dto, commentId, userId);
    }

    @Transactional
    public void deleteAdmin(Long commentId) throws ConditionsException {
        CommentUpdateDto dto = CommentUpdateDto.builder()
                .deleted(true)
                .isAdmin(true)
                .build();
        update(dto, commentId, null);
    }

    private boolean isAuthorComment(User author, Long userId) {
        if (userId == null || author == null) {
            return false;
        }
        return Objects.equals(author.getId(), userId);
    }


    @Transactional(readOnly = true)
    public List<CommentDto> findAllCommentsForEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Отсутствует событие с id=" + eventId);
        }

        log.info("Получаем комментарии по событию с id={}", eventId);
        var comments = commentRepository.findCommentsByEvent(eventId).orElse(new ArrayList<>());

        log.info("Возвращаем {} комментариев события с id={}", comments.size(), eventId);
        return comments.stream()
                .map(commentMapper::toDto)
                .toList();
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден."));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }
}
