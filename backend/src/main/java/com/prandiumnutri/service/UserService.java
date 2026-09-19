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
        return listAll(null, null);
    }

    /**
     * Fase 6B: busca por nome/email/cpf/crn + ordenacao ('name' asc | 'created' desc).
     * @param q termo de busca (case-insensitive; casa em nome, email, cpf normalizado, crn); null = tudo
     * @param sort 'name' ou 'created' (default = 'created')
     */
    @Transactional(readOnly = true)
    public List<AdminUserDto> listAll(String q, String sort) {
        var stream = userRepository.findAll().stream();
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            String digits = q.replaceAll("\\D", "");
            stream = stream.filter(u ->
                (u.getName() != null && u.getName().toLowerCase().contains(needle))
             || (u.getEmail() != null && u.getEmail().toLowerCase().contains(needle))
             || (!digits.isEmpty() && u.getCpf() != null && u.getCpf().contains(digits))
             || (u.getCrn() != null && u.getCrn().toLowerCase().contains(needle))
            );
        }
        java.util.Comparator<User> comp = !"created".equalsIgnoreCase(sort)
                ? java.util.Comparator.comparing(u -> u.getName() == null ? "" : u.getName().toLowerCase())
                : java.util.Comparator.comparing(
                        (User u) -> u.getCreatedAt() == null ? java.time.LocalDateTime.MIN : u.getCreatedAt()
                  ).reversed();
        return stream.sorted(comp).map(this::toAdminDto).toList();
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
        return new UserDto(u.getId(), u.getEmail(),
                com.prandiumnutri.support.CpfUtil.mask(u.getCpf()),
                u.getCrn(),
                u.getName(), u.getSex(), u.getBirthDate(),
                u.getWeightKg(), u.getHeightCm(), u.getActivityLevel(), u.getGoal(), u.getPreferences(),
                u.getMedications(), u.isHasBariatricSurgery(), u.getPreExistingConditions(), u.getAllergies());
    }

    public AdminUserDto toAdminDto(User u) {
        return new AdminUserDto(u.getId(), u.getEmail(),
                com.prandiumnutri.support.CpfUtil.mask(u.getCpf()),
                u.getCrn(),
                u.getName(), u.getRole(),
                u.getProvider(), u.isProfileCompleted(), u.isActive());
    }
}
