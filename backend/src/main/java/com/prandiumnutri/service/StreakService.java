package com.prandiumnutri.service;

import com.prandiumnutri.dto.StreakInfoDto;
import com.prandiumnutri.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fase 10C: streaks + badges baseados em datas com refeicao registrada.
 * <p>Deriva tudo direto da tabela {@code meals} — nao precisa persistir nada.
 * Como os thresholds (3/7/14/30/60/100) e o calculo linear sao cheap, cabe fazer
 * on-the-fly em cada request do dashboard.
 */
@Service
@RequiredArgsConstructor
public class StreakService {

    /** Marcos que viram badge (em dias). Ordem crescente. */
    private static final int[] THRESHOLDS = {3, 7, 14, 30, 60, 100};

    private final MealRepository mealRepository;

    @Transactional(readOnly = true)
    public StreakInfoDto compute(Long userId) {
        List<LocalDate> dates = mealRepository.distinctDatesDesc(userId);
        if (dates.isEmpty()) {
            return new StreakInfoDto(0, 0, false, List.of());
        }
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        boolean todayLogged = dates.contains(today);

        // current streak: parte da data mais recente (hoje ou ontem) e conta pra tras.
        int current = 0;
        LocalDate cursor = dates.get(0);
        if (cursor.equals(today) || cursor.equals(yesterday)) {
            current = 1;
            for (int i = 1; i < dates.size(); i++) {
                if (dates.get(i).equals(cursor.minusDays(1))) {
                    current++;
                    cursor = dates.get(i);
                } else break;
            }
        }

        // longest: varre uma vez ordenado desc, conta corridas.
        int longest = 1;
        int run = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).equals(dates.get(i - 1).minusDays(1))) {
                run++;
                if (run > longest) longest = run;
            } else {
                run = 1;
            }
        }

        List<String> badges = new ArrayList<>();
        for (int t : THRESHOLDS) {
            if (longest >= t) badges.add("streak_" + t);
        }
        return new StreakInfoDto(current, longest, todayLogged, badges);
    }
}
