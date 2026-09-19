package com.prandiumnutri.dto;

import java.util.List;

/**
 * Envelope de paginacao (Fase 7B).
 * page e size sao 0-based e devolvidos junto pra o cliente reconstruir estado.
 */
public record PageDto<T>(
        List<T> items,
        long total,
        int page,
        int size
) {
    public static <T> PageDto<T> of(List<T> all, int page, int size) {
        int p = Math.max(0, page);
        int s = size <= 0 ? 10 : size;
        int from = Math.min(p * s, all.size());
        int to = Math.min(from + s, all.size());
        return new PageDto<>(all.subList(from, to), all.size(), p, s);
    }
}
