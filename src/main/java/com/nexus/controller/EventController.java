package com.nexus.controller;

import com.nexus.model.Event;
import com.nexus.model.Registration;
import com.nexus.model.User;
import com.nexus.repository.EventRepository;
import com.nexus.repository.RegistrationRepository;
import com.nexus.repository.UserRepository;
import com.nexus.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService; // INJECTED EMAIL SERVICE

    public EventController(EventRepository eventRepository, RegistrationRepository registrationRepository, UserRepository userRepository, EmailService emailService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public String showEventsDashboard(Model model) {
        List<Event> activeEvents = eventRepository.findAll().stream()
                .filter(Event::isActive)
                .collect(Collectors.toList());
        
        model.addAttribute("events", activeEvents);
        return "events-dashboard";
    }

    @GetMapping("/register/{id}")
    public String showRegistrationForm(@PathVariable("id") Long id, Model model) {
        Event event = eventRepository.findById(id).orElseThrow();
        
        long currentAttendees = event.getRegistrations() != null ? 
            event.getRegistrations().stream().filter(r -> !r.isWaitlisted()).count() : 0;
        
        boolean isFull = event.getMaxCapacity() > 0 && currentAttendees >= event.getMaxCapacity();

        model.addAttribute("event", event);
        model.addAttribute("registration", new Registration());
        model.addAttribute("isFull", isFull); 
        model.addAttribute("alreadyRegistered", false); 
        
        return "event-register";
    }

    @PostMapping("/register")
    public String submitRegistration(@ModelAttribute Registration registration, @RequestParam("eventId") Long eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElseThrow();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);

            if (currentUser == null) return "redirect:/events/register/" + eventId + "?error=true";
            if (registrationRepository.existsByEventAndUser(event, currentUser)) {
                return "redirect:/events/register/" + eventId + "?error=true";
            }

            long currentAttendees = event.getRegistrations() != null ? 
                event.getRegistrations().stream().filter(r -> !r.isWaitlisted()).count() : 0;

            boolean isWaitlisted = false;
            if (event.getMaxCapacity() > 0 && currentAttendees >= event.getMaxCapacity()) {
                registration.setWaitlisted(true);
                isWaitlisted = true;
            }

            registration.setEvent(event);
            registration.setUser(currentUser); 
            
            // SAVE TO DATABASE
            registrationRepository.save(registration);

            // --- NEW: FIRE OFF THE EMAIL IN THE BACKGROUND ---
            emailService.sendRegistrationEmail(
                registration.getStudentEmail(),
                registration.getStudentName(),
                event.getTitle(),
                registration.getTicketNumber(),
                isWaitlisted
            );

            if (isWaitlisted) {
                return "redirect:/events/register/" + eventId + "?waitlisted=true";
            } else {
                return "redirect:/events/register/" + eventId + "?success=true";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/events/register/" + eventId + "?error=true";
        }
    }

    @GetMapping("/my-tickets")
    public String showMyTickets(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);

        if (currentUser != null) {
            List<Registration> myRegistrations = registrationRepository.findByUser(currentUser);
            model.addAttribute("registrations", myRegistrations);
        }
        return "my-tickets";
    }
}