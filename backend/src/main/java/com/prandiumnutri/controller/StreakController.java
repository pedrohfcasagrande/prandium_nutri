package com.prandiumnutri.controller;

import com.prandiumnutri.dto.StreakInfoDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/streaks")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;
    private final CurrentUser currentUser;

    @GetMapping
    public StreakInfoDto get() {
        return streakService.compute(currentUser.id());
    }
}
