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
import ru.practicum.ewm.feign.user.UserClient;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.repository.CommentRepository;

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
    private final UserClient userClient;
    private final CommentMapper commentMapper;

    @Transactional(readOnly = true)
    public CommentDto findById(Long id) {
        Comment comment = getCommentOrThrow(id);
        return commentMapper.toDto(comment, userClient.getNameById(comment.getAuthorId()));
    }

    @Transactional
    @Validated(CreateValidation.class)
    public CommentDto create(@Valid CommentUpdateDto entity, Long userId) {
        var comment = Comment.builder()
                .authorId(userId)
                .eventId(entity.getEventId())
                .text(entity.getText())
                .deleted(false)
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();
        log.info("Создание нового комментария к событию с id={} пользователем с id={}", entity.getEventId(), userId);
        comment = commentRepository.save(comment);
        return commentMapper.toDto(comment, userClient.getNameById(comment.getAuthorId()));
    }

    @Transactional
    @Validated(UpdateValidation.class)
    public CommentDto update(@Valid CommentUpdateDto dto, Long commentId, Long userId) throws ConditionsException {
        var comment = getCommentOrThrow(commentId);
        if (!dto.getIsAdmin() && !Objects.equals(comment.getAuthorId(), userId)) {
            throw new ConditionsException("Вы не можете редактировать данный комментарий");
        }
        comment = commentMapper.mapEntityFromDto(comment, dto);
        log.info("Обновление комментария к событию с id={} пользователем с id {}", comment.getEventId(), userId);
        comment = commentRepository.save(comment);
        return commentMapper.toDto(comment, userClient.getNameById(comment.getAuthorId()));
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

    @Transactional(readOnly = true)
    public List<CommentDto> findAllCommentsForEvent(Long eventId) {
        log.info("Получаем комментарии по событию с id={}", eventId);
        var comments = commentRepository.findAllByEventIdAndDeletedFalseOrderByCreatedDesc(eventId).orElse(new ArrayList<>());

        log.info("Возвращаем {} комментариев события с id={}", comments.size(), eventId);
        return comments.stream()
                .map(com -> commentMapper.toDto(com, userClient.getNameById(com.getAuthorId())))
                .toList();
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден."));
    }
}
