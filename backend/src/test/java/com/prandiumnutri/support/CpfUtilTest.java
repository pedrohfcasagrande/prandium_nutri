package com.prandiumnutri.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfUtilTest {

    @Test
    void mask_normalDigits_hidesMiddle() {
        assertThat(CpfUtil.mask("12345678901")).isEqualTo("123.***.***-01");
    }

    @Test
    void mask_withFormatting_stripsAndMasks() {
        assertThat(CpfUtil.mask("123.456.789-01")).isEqualTo("123.***.***-01");
    }

    @Test
    void mask_nullOrShort_returnsPlaceholder() {
        assertThat(CpfUtil.mask(null)).isEqualTo("***");
        assertThat(CpfUtil.mask("")).isEqualTo("***");
        assertThat(CpfUtil.mask("123")).isEqualTo("***");
    }

    @Test
    void normalize_stripsNonDigits() {
        assertThat(CpfUtil.normalize("  123.456.789-01 ")).isEqualTo("12345678901");
        assertThat(CpfUtil.normalize("abc-def")).isEqualTo("");
        assertThat(CpfUtil.normalize(null)).isNull();
    }
}
