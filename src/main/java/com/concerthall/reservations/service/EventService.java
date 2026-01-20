package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.dto.request.CreateEventRequest;
import com.concerthall.reservations.dto.request.UpdateEventRequest;
import com.concerthall.reservations.dto.request.UpdateEventStatusRequest;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.EventSalesResponse;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(boolean customerView) {
        List<Event> events;
        if (customerView) {
            events = eventRepository.findByStatus(EventStatus.PUBLISHED);
        } else {
            events = eventRepository.findAll();
        }

        return events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toResponse(event);
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .eventDateTime(request.getEventDateTime())
                .capacity(request.getCapacity())
                .price(request.getPrice())
                .status(request.getStatus())
                .build();

        event = eventRepository.save(event);
        log.info("Event created: {} with ID {}", event.getName(), event.getId());

        return toResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDateTime() != null) {
            event.setEventDateTime(request.getEventDateTime());
        }
        if (request.getCapacity() != null) {
            event.setCapacity(request.getCapacity());
        }
        if (request.getPrice() != null) {
            event.setPrice(request.getPrice());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        event = eventRepository.save(event);
        log.info("Event updated: {}", event.getId());

        return toResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        eventRepository.delete(event);
        log.info("Event deleted: {}", id);
    }

    @Transactional
    public EventResponse updateEventStatus(Long id, UpdateEventStatusRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        event.setStatus(request.getStatus());
        event = eventRepository.save(event);
        log.info("Event {} status updated to {}", id, request.getStatus());

        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public EventSalesResponse getEventSales(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        long ticketsSold = ticketRepository.countByEventIdAndStatus(id, TicketStatus.RESERVED);
        long availableTickets = event.getCapacity() - ticketsSold;
        BigDecimal revenue = event.getPrice().multiply(BigDecimal.valueOf(ticketsSold));
        double occupancyRate = (ticketsSold * 100.0) / event.getCapacity();

        return EventSalesResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .capacity(event.getCapacity())
                .ticketsSold(ticketsSold)
                .availableTickets(availableTickets)
                .revenue(revenue)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .build();
    }

    private EventResponse toResponse(Event event) {
        long reservedCount = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.RESERVED);
        long availableTickets = event.getCapacity() - reservedCount;

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .eventDateTime(event.getEventDateTime())
                .capacity(event.getCapacity())
                .price(event.getPrice())
                .status(event.getStatus().name())
                .availableTickets(availableTickets)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
