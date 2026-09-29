package com.prandiumnutri.service;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.ClientChecklistDto;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.MealRepository;
import com.prandiumnutri.repository.ProgressEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Fase 10D: checklist de onboarding derivado do estado atual.
 * Nao ha persistencia — cada request recomputa. Consequencia: se cliente
 * remover a ultima refeicao, o passo "primeira refeicao" volta a false.
 * Trade-off aceito: evita migration + tabela + estado corruptivel.
 */
@Service
@RequiredArgsConstructor
public class ClientChecklistService {

    private final UserService userService;
    private final ClientAssessmentRepository assessmentRepository;
    private final MealRepository mealRepository;
    private final ProgressEntryRepository progressRepository;

    @Transactional(readOnly = true)
    public ClientChecklistDto compute(Long userId) {
        User user = userService.getOrThrow(userId);

        boolean profileFilled = user.getWeightKg() != null
                && user.getHeightCm() != null
                && user.getGoal() != null;

        Optional<ClientAssessment> ficha = assessmentRepository
                .findByClientIdOrderByUpdatedAtDesc(userId).stream()
                .filter(a -> a.getTargetCalories() != null)
                .findFirst();
        boolean goalsPrescribed = ficha.isPresent();

        boolean firstMealLogged = mealRepository.countByUserId(userId) > 0;
        boolean firstProgressLogged = progressRepository.countByUserId(userId) > 0;

        int total = 4;
        int done = (profileFilled ? 1 : 0)
                + (goalsPrescribed ? 1 : 0)
                + (firstMealLogged ? 1 : 0)
                + (firstProgressLogged ? 1 : 0);

        return new ClientChecklistDto(profileFilled, goalsPrescribed,
                firstMealLogged, firstProgressLogged, done, total);
    }
}
