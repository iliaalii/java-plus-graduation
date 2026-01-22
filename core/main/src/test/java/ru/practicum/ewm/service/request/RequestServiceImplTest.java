package ru.practicum.ewm.service.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.constant.EventState;
import ru.practicum.ewm.constant.RequestStatus;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.core.exception.ConflictException;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Request;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestMapper mapper;

    @InjectMocks
    private RequestService requestService;

    private User user;

    private Event event;
    private Request request;

    @BeforeEach
    void setUp() {
        User owner;
        user = new User();
        user.setId(1L);
        user.setName("user");
        user.setEmail("test@mail.com");

        owner = new User();
        owner.setId(2L);
        owner.setName("owner");
        owner.setEmail("owner@mail.com");

        event = Event.builder()
                .id(1L)
                .initiator(owner)
                .state(EventState.PUBLISHED)
                .participantLimit(5L)
                .requestModeration(true)
                .build();
        request = Request.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    void createTest() throws ConditionsException, ConflictException {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(event.getId(), user.getId()))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenReturn(request);
        when(mapper.toDto(any())).thenReturn(new ParticipationRequestDto());

        ParticipationRequestDto dto = requestService.create(user.getId(), event.getId());

        assertNotNull(dto);
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void createDuplicateTest() {
        when(requestRepository.findByEventIdAndRequesterId(event.getId(), user.getId()))
                .thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () ->
                requestService.create(user.getId(), event.getId()));
    }

    @Test
    void getRequestsByUserTest() {
        when(userRepository.existsById(user.getId())).thenReturn(true);
        when(requestRepository.findByRequesterId(user.getId()))
                .thenReturn(List.of(request));
        when(mapper.toDto(any())).thenReturn(new ParticipationRequestDto());

        List<ParticipationRequestDto> requests = requestService.getRequestsByUser(user.getId());

        assertEquals(1, requests.size());
    }

    @Test
    void cancelRequestTest() throws ConditionsException {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);
        when(mapper.toDto(any())).thenReturn(new ParticipationRequestDto());

        ParticipationRequestDto dto = requestService.cancelRequest(user.getId(), request.getId());

        assertNotNull(dto);
        assertEquals(RequestStatus.CANCELED, request.getStatus());
    }

    @Test
    void getRequestsForEventOwnerTest() throws ConditionsException {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(requestRepository.findByEventId(event.getId())).thenReturn(List.of(request));
        when(mapper.toDto(any())).thenReturn(new ParticipationRequestDto());

        List<ParticipationRequestDto> dtos = requestService.getRequestsForEventOwner(
                event.getInitiator().getId(), event.getId());

        assertEquals(1, dtos.size());
    }

    @Test
    void updateRequestStatusConfirmTest() throws ConditionsException, ConflictException {
        EventRequestStatusUpdateRequest updateDto =
                new EventRequestStatusUpdateRequest(List.of(request.getId()), RequestStatus.CONFIRMED);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.findAllByIdInAndStatus(any(), eq(RequestStatus.PENDING)))
                .thenReturn(List.of(request));
        when(requestRepository.saveAll(any())).thenReturn(List.of(request));
        when(mapper.toDto(any())).thenReturn(new ParticipationRequestDto());

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatus(event.getInitiator().getId(), event.getId(), updateDto);

        assertEquals(1, result.getConfirmedRequests().size());
    }
}