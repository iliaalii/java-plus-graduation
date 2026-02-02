package ru.practicum.ewm.dto.compilation;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationUpdateDto extends CompilationCommonDto {

    private List<Long> events;
}
