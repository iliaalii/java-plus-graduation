package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.constant.EventState;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "created")
    private LocalDateTime createdOn;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private Boolean paid;

    @Column(name = "participant_limit", nullable = false)
    private Long participantLimit;

    @Column(name = "published")
    private LocalDateTime publishedOn;

    @Column(name = "moderate", nullable = false)
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    private EventState state;

    @Column(nullable = false, length = 120)
    private String title;
}

