package com.nexus.repository;

import com.nexus.model.Event;
import com.nexus.model.Registration;
import com.nexus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    
    boolean existsByEventAndUser(Event event, User user);
    
    List<Registration> findByUser(User user);
    
    // NEW: Find a specific registration by its unique ticket number for Live Check-in
    Optional<Registration> findByTicketNumber(String ticketNumber);
}