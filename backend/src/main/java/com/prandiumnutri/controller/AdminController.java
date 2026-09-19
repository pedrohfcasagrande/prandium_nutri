package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AdminUserDto;
import com.prandiumnutri.dto.PageDto;
import com.prandiumnutri.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping
    public PageDto<AdminUserDto> listUsers(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return PageDto.of(userService.listAll(q, sort), page, size);
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
