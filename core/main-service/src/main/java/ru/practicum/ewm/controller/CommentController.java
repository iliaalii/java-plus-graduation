package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.CommentUpdateDto;
import ru.practicum.ewm.service.CommentService;

@RestController
@RequestMapping(path = "/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto findById(@PathVariable Long id) throws ConditionsException {
        return commentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(
            @RequestBody CommentUpdateDto dto,
            @RequestHeader("X-User-Id") Long userId) throws ConditionsException {
        return commentService.create(dto, userId);
    }

    @PatchMapping(path = "/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto update(
            @RequestBody CommentUpdateDto dto,
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
}
