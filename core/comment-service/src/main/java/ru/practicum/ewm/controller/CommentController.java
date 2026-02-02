package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.CommentUpdateDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

@RestController
@RequestMapping(path = "/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto findById(@PathVariable Long id) {
        return commentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(
            @RequestBody @Valid CommentUpdateDto dto,
            @RequestHeader("X-User-Id") Long userId) {
        return commentService.create(dto, userId);
    }

    @PatchMapping(path = "/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto update(
            @RequestBody @Valid CommentUpdateDto dto,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId) throws ConditionsException {
        return commentService.update(dto, commentId, userId);
    }

    @DeleteMapping(path = "/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId) throws ConditionsException {
        commentService.delete(commentId, userId);
    }

    @GetMapping("/{id}/all")
    public List<CommentDto> findAllCommentsForEvent(@PathVariable Long id) {
        return commentService.findAllCommentsForEvent(id);
    }
}
