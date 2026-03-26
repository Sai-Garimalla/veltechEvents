package com.nexus.controller;

import com.nexus.model.Event;
import com.nexus.model.Registration;
import com.nexus.repository.EventRepository;
import com.nexus.repository.RegistrationRepository;
import com.nexus.service.EventService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public AdminController(EventService eventService, EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    // --- UPDATED: Cross-Platform File Upload Path ---
    private Path getUploadDirectory() throws IOException {
        String currentDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(currentDir, "uploads");
        
        // Creates the folder if it doesn't exist yet
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    @GetMapping("/manage-events")
    public String manageEvents(Model model) {
        // Only fetch active events (hides soft-deleted ones)
        List<Event> activeEvents = eventRepository.findAll().stream()
                .filter(Event::isActive)
                .collect(Collectors.toList());
                
        model.addAttribute("events", activeEvents);
        model.addAttribute("newEvent", new Event());
        model.addAttribute("registrations", registrationRepository.findAll()); 
        return "admin-dashboard"; 
    }

    @PostMapping("/add-event")
    public String addEvent(@ModelAttribute Event event, @RequestParam("imageFile") MultipartFile imageFile) {
        if (!imageFile.isEmpty()) {
            try {
                Path uploadPath = getUploadDirectory();
                String originalName = imageFile.getOriginalFilename();
                String fileExtension = (originalName != null && originalName.contains(".")) ? originalName.substring(originalName.lastIndexOf(".")) : "";

                String cleanEventTitle = event.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
                String safeDate = event.getEventDate().toString().replaceAll("[^a-zA-Z0-9]", "_");
                String fileName = cleanEventTitle + "_" + safeDate + fileExtension;
                
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                event.setImageUrl("/uploads/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        eventService.saveEvent(event);
        return "redirect:/admin/manage-events?added=true";
    }

    // --- LIVE CHECK-IN SYSTEM ---
    @PostMapping("/check-in")
    public String processCheckIn(@RequestParam("ticketNumber") String ticketNumber) {
        Optional<Registration> regOpt = registrationRepository.findByTicketNumber(ticketNumber.trim().toUpperCase());
        
        if (regOpt.isPresent()) {
            Registration reg = regOpt.get();
            if (reg.isWaitlisted()) return "redirect:/admin/manage-events?error=waitlisted";
            if (reg.isCheckedIn()) return "redirect:/admin/manage-events?error=already_checked_in";
            
            reg.setCheckedIn(true);
            registrationRepository.save(reg);
            return "redirect:/admin/manage-events?success=checked_in&ticket=" + ticketNumber;
        }
        return "redirect:/admin/manage-events?error=invalid_ticket";
    }

    // --- CSV EXPORT FEATURE ---
    @GetMapping("/export-csv/{id}")
    public void exportCSV(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Event event = eventRepository.findById(id).orElseThrow();
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"registrations_" + event.getTitle().replaceAll(" ", "_") + ".csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Ticket ID,Student Name,VTU ID,Department,YOP,Email,Phone,Status,Checked In");

        List<Registration> registrations = registrationRepository.findAll().stream()
                .filter(reg -> reg.getEvent().getId().equals(id))
                .collect(Collectors.toList());

        for (Registration reg : registrations) {
            String status = reg.isWaitlisted() ? "Waitlisted" : "Confirmed";
            String checkedIn = reg.isCheckedIn() ? "Yes" : "No";
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    reg.getTicketNumber(), reg.getStudentName(), reg.getVtuId(),
                    reg.getDepartment(), reg.getYop(), reg.getStudentEmail(),
                    reg.getPhoneNumber(), status, checkedIn);
        }
    }

    @GetMapping("/delete-registration/{id}")
    public String deleteRegistration(@PathVariable("id") Long id) {
        registrationRepository.deleteById(id);
        return "redirect:/admin/manage-events?added=true"; 
    }

    @GetMapping("/delete-event/{id}")
    public String deleteEvent(@PathVariable("id") Long id) {
        Event event = eventRepository.findById(id).orElseThrow();
        event.setActive(false); // Soft Delete
        eventRepository.save(event);
        return "redirect:/admin/manage-events?added=true";
    }

    @GetMapping("/edit-event/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("event", eventRepository.findById(id).orElseThrow());
        return "admin-edit-event";
    }

    @PostMapping("/update-event/{id}")
    public String updateEvent(@PathVariable("id") Long id, @ModelAttribute Event eventDetails, @RequestParam("imageFile") MultipartFile imageFile) {
        Event existingEvent = eventRepository.findById(id).orElseThrow();
        String cleanEventTitle = eventDetails.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
        String safeDate = eventDetails.getEventDate().toString().replaceAll("[^a-zA-Z0-9]", "_");
        String newBaseName = cleanEventTitle + "_" + safeDate;

        try {
            Path uploadPath = getUploadDirectory();
            String oldFileName = existingEvent.getImageUrl() != null ? existingEvent.getImageUrl().substring(existingEvent.getImageUrl().lastIndexOf("/") + 1) : null;

            if (!imageFile.isEmpty()) {
                if (oldFileName != null) Files.deleteIfExists(uploadPath.resolve(oldFileName));
                String originalName = imageFile.getOriginalFilename();
                String fileExtension = (originalName != null && originalName.contains(".")) ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String fileName = newBaseName + fileExtension;
                Files.copy(imageFile.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                existingEvent.setImageUrl("/uploads/" + fileName);
            } else if (oldFileName != null) {
                String fileExtension = oldFileName.contains(".") ? oldFileName.substring(oldFileName.lastIndexOf(".")) : "";
                String newFileName = newBaseName + fileExtension;
                if (!oldFileName.equals(newFileName)) {
                    Path oldFilePath = uploadPath.resolve(oldFileName);
                    Path newFilePath = uploadPath.resolve(newFileName);
                    if (Files.exists(oldFilePath)) {
                        Files.move(oldFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                        existingEvent.setImageUrl("/uploads/" + newFileName);
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        
        existingEvent.setTitle(eventDetails.getTitle());
        existingEvent.setEventDate(eventDetails.getEventDate());
        existingEvent.setLocation(eventDetails.getLocation());
        existingEvent.setMaxCapacity(eventDetails.getMaxCapacity());
        
        eventRepository.save(existingEvent);
        return "redirect:/admin/manage-events?added=true";
    }
}