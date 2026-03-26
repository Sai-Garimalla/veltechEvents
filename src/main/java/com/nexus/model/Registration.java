package com.nexus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String studentName;
    private String vtuId;
    private String department;
    private String yop; // Year of Passing
    private String studentEmail;
    private String phoneNumber;
    private String interested; // Yes / No

    private LocalDateTime registrationDate = LocalDateTime.now();

    // --- NEW PHASE 1 FEATURES ---
    
    private boolean isCheckedIn = false;
    private boolean isWaitlisted = false;
    
    // Generates a random 8-character unique ticket ID when created
    @Column(unique = true, updatable = false)
    private String ticketNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); 

    // --- GETTERS ---
    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public User getUser() { return user; }
    public String getStudentName() { return studentName; }
    public String getVtuId() { return vtuId; }
    public String getDepartment() { return department; }
    public String getYop() { return yop; }
    public String getStudentEmail() { return studentEmail; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getInterested() { return interested; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    
    public boolean isCheckedIn() { return isCheckedIn; }
    public boolean isWaitlisted() { return isWaitlisted; }
    public String getTicketNumber() { return ticketNumber; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setEvent(Event event) { this.event = event; }
    public void setUser(User user) { this.user = user; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setVtuId(String vtuId) { this.vtuId = vtuId; }
    public void setDepartment(String department) { this.department = department; }
    public void setYop(String yop) { this.yop = yop; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setInterested(String interested) { this.interested = interested; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public void setCheckedIn(boolean checkedIn) { isCheckedIn = checkedIn; }
    public void setWaitlisted(boolean waitlisted) { isWaitlisted = waitlisted; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
}