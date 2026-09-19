package com.prandiumnutri.controller;

import com.prandiumnutri.dto.HealthProfileDto;
import com.prandiumnutri.dto.UserDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.UserService;
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
