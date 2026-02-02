package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.constant.EventState;
import ru.practicum.ewm.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventNewDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.EventUpdateDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;

import java.util.List;

@Mapper(config = CommonMapperConfiguration.class, uses = {LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "comments", source = "comments")
    EventFullDto eventToFullDto(Event event,
                                Long confirmedRequests,
                                Long views,
                                List<CommentDto> comments,
                                CategoryDto category,
                                UserDto initiator);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views", source = "views")
    EventShortDto eventToShortDto(Event event,
                                  Long confirmedRequests,
                                  Long views,
                                  CategoryDto category,
                                  UserDto initiator);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "annotation", source = "dto.annotation")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "initiatorId", source = "initiatorId")
    @Mapping(target = "eventDate", source = "dto.eventDate")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "paid", source = "dto.paid")
    @Mapping(target = "participantLimit", source = "dto.participantLimit")
    @Mapping(target = "publishedOn", source = "dto.publishedOn")
    @Mapping(target = "requestModeration", source = "dto.requestModeration")
    @Mapping(target = "state", source = "dto.state")
    @Mapping(target = "title", source = "dto.title")
    Event toEntityFromDto(EventNewDto dto, Long initiatorId, Long categoryId, Location location);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "annotation", source = "dto.annotation")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "eventDate", source = "dto.eventDate")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "paid", source = "dto.paid")
    @Mapping(target = "participantLimit", source = "dto.participantLimit")
    @Mapping(target = "requestModeration", source = "dto.requestModeration")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "title", source = "dto.title")
    void updateEntityFromDto(@MappingTarget Event event, EventUpdateDto dto, Long categoryId, Location location, EventState state);
}
