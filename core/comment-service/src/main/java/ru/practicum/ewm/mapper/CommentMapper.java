package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.CommentUpdateDto;
import ru.practicum.ewm.model.Comment;

@Mapper(config = CommonMapperConfiguration.class)
public interface CommentMapper {

    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "event", source = "entity.eventId")
    CommentDto toDto(Comment entity, String authorName);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "updated", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "text", source = "dto.text")
    @Mapping(target = "deleted", source = "dto.deleted")
    Comment mapEntityFromDto(@MappingTarget Comment entity, CommentUpdateDto dto);

}
