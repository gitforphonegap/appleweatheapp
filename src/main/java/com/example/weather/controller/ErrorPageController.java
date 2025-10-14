package com.example.weather.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorPageController {
    @GetMapping("/error-page")
    public String showErrorPage(@RequestParam(name = "error", required = false) String error, Model model) {
        model.addAttribute("errorMessage", error != null ? error : "An unknown error occurred.");
        return "error";
    }
}
