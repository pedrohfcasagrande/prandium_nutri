package com.prandiumnutri.service;

import com.prandiumnutri.domain.User;
import com.prandiumnutri.domain.WaterIntake;
import com.prandiumnutri.dto.WaterIntakeDto;
import com.prandiumnutri.repository.WaterIntakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fase 10B: registro rapido de hidratacao do cliente.
 * Cliente aperta preset (200/300/500 ml) e a row entra na data corrente.
 * Nao ha "edicao"; para corrigir, deletar e re-registrar.
 */
@Service
@RequiredArgsConstructor
public class WaterIntakeService {

    private static final int MAX_ML_PER_LOG = 3000;

    private final WaterIntakeRepository repository;
    private final UserService userService;

    @Transactional
    public WaterIntakeDto log(Long userId, int amountMl) {
        if (amountMl <= 0 || amountMl > MAX_ML_PER_LOG) {
            throw new IllegalArgumentException("amountMl fora do intervalo (1.." + MAX_ML_PER_LOG + ")");
        }
        User user = userService.getOrThrow(userId);
        WaterIntake saved = repository.save(WaterIntake.builder()
                .user(user)
                .date(LocalDate.now())
                .amountMl(amountMl)
                .loggedAt(LocalDateTime.now())
                .build());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long userId, Long intakeId) {
        WaterIntake w = repository.findById(intakeId)
                .orElseThrow(() -> new IllegalArgumentException("Registro nao encontrado"));
        if (!w.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Nao pode remover registro de outro usuario");
        }
        repository.delete(w);
    }

    @Transactional(readOnly = true)
    public List<WaterIntakeDto> listByDate(Long userId, LocalDate date) {
        return repository.findByUserIdAndDateOrderByLoggedAtAsc(userId, date).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long totalMl(Long userId, LocalDate date) {
        return repository.sumMlByUserAndDate(userId, date);
    }

    private WaterIntakeDto toDto(WaterIntake w) {
        return new WaterIntakeDto(w.getId(), w.getDate(), w.getAmountMl(), w.getLoggedAt());
    }
}
