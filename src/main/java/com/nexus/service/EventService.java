package com.nexus.service;

import com.nexus.model.Event;
import com.nexus.repository.EventRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventService {
    
    private final EventRepository eventRepository;
    
    public EventService(EventRepository eventRepository) { 
        this.eventRepository = eventRepository; 
    }
    
    public List<Event> getAllEvents() { 
        return eventRepository.findAll(); 
    }
    
    // Added null check to satisfy the Java Language Server's strict type safety
    public void saveEvent(Event event) { 
        if (event != null) {
            eventRepository.save(event); 
        }
    }
}