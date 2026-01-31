package ru.practicum.ewm.dto.compilation;

import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.practicum.ewm.dto.event.EventShortDto;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationFullDto extends CompilationCommonDto {

    private List<EventShortDto> events;
}
