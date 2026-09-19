package com.prandiumnutri.service;

import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.AdminUserDto;
import com.prandiumnutri.dto.HealthProfileDto;
import com.prandiumnutri.dto.UserDto;
import com.prandiumnutri.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public UserDto updateHealthProfile(User user, HealthProfileDto dto) {
        user.setSex(dto.sex());
        user.setBirthDate(dto.birthDate());
        user.setWeightKg(dto.weightKg());
        user.setHeightCm(dto.heightCm());
        user.setActivityLevel(dto.activityLevel());
        user.setGoal(dto.goal());
        user.setPreferences(dto.preferences());
        user.setMedications(dto.medications());
        user.setHasBariatricSurgery(Boolean.TRUE.equals(dto.hasBariatricSurgery()));
        user.setPreExistingConditions(dto.preExistingConditions());
        user.setAllergies(dto.allergies());
        user.setProfileCompleted(true);
        return toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listAll() {
        return userRepository.findAll().stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public AdminUserDto setActive(Long id, boolean active) {
        User u = getOrThrow(id);
        u.setActive(active);
        return toAdminDto(userRepository.save(u));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    public UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getName(), u.getSex(), u.getBirthDate(),
                u.getWeightKg(), u.getHeightCm(), u.getActivityLevel(), u.getGoal(), u.getPreferences(),
                u.getMedications(), u.isHasBariatricSurgery(), u.getPreExistingConditions(), u.getAllergies());
    }

    public AdminUserDto toAdminDto(User u) {
        return new AdminUserDto(u.getId(), u.getEmail(), u.getName(), u.getRole(),
                u.getProvider(), u.isProfileCompleted(), u.isActive());
    }
}
