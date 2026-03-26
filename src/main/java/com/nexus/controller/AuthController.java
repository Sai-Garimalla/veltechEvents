package com.nexus.controller;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        // Hash the password securely
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // CRITICAL FIX: Set default role to USER. 
        // This completely hides the Admin Panel from new signups.
        user.setRole("ROLE_USER"); 
        
        userRepository.save(user);
        
        return "redirect:/login?registered=true";
    }
}