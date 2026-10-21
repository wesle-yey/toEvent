package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.EventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final EventService eventService;

    public PageController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String homePage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("username", user.getName());
        model.addAttribute("events", eventService.getAllEventsByUser(user.getId()));
        return "index";
    }
}
