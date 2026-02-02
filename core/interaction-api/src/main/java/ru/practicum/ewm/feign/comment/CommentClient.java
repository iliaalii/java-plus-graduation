package ru.practicum.ewm.feign.comment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.dto.comment.CommentDto;

import java.util.List;

@FeignClient(name = "comment-service", fallbackFactory = CommentClientFallbackFactory.class)
public interface CommentClient {
    @GetMapping("/comments/{id}/all")
    List<CommentDto> findAllCommentsForEvent(@PathVariable Long id);
}
