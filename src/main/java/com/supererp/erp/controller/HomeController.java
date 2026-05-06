package com.supererp.erp.controller;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/home")
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping
    public String home(Authentication auth, Model model) {
        AppUser user = userService.getByUsername(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        
        // Determine greeting based on time of day
        int hour = LocalDateTime.now().getHour();
        String greeting = "Good Evening";
        if (hour < 12) greeting = "Good Morning";
        else if (hour < 17) greeting = "Good Afternoon";
        model.addAttribute("greeting", greeting);

        return "home";
    }
}
