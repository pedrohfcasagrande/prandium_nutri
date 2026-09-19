package com.nutritracker.controller;

import com.nutritracker.dto.HealthProfileDto;
import com.nutritracker.dto.UserDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    public UserDto me() {
        return userService.toDto(currentUser.get());
    }

    @PutMapping("/me/health-profile")
    public UserDto saveHealthProfile(@Valid @RequestBody HealthProfileDto dto) {
        return userService.updateHealthProfile(currentUser.get(), dto);
    }
}
