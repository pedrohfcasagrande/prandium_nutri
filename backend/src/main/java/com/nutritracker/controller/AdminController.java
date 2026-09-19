package com.nutritracker.controller;

import com.nutritracker.dto.AdminUserDto;
import com.nutritracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping
    public List<AdminUserDto> listUsers() {
        return userService.listAll();
    }

    @PutMapping("/{id}/active")
    public AdminUserDto setActive(@PathVariable Long id, @RequestParam boolean value) {
        return userService.setActive(id, value);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
