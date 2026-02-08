package ru.practicum.ewm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
public class UserAction {

    @NotBlank
    private Long userId;

    @NotBlank
    private Long eventId;

    @NotNull
    private ActionType actionType;

    private Instant timestamp = Instant.now();

}
