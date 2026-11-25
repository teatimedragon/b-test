package com.somecompany.marketplace.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageRequestParamsTest {

    @Test
    void defaults() {
        PageRequestParams p = PageRequestParams.parseOrThrow(null, null);
        assertThat(p.page()).isZero();
        assertThat(p.size()).isEqualTo(20);
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> PageRequestParams.parseOrThrow("-1", "101"))
                .isInstanceOfSatisfying(ParameterValidationException.class,
                        e -> assertThat(e.getErrors()).extracting(FieldError::field).containsExactly("page", "size"));
    }

    @Test
    void slicesAndReturnsEmptyPastTheEnd() {
        List<Integer> all = List.of(1, 2, 3, 4, 5);
        assertThat(new PageRequestParams(0, 2).slice(all)).containsExactly(1, 2);
        assertThat(new PageRequestParams(2, 2).slice(all)).containsExactly(5);
        assertThat(new PageRequestParams(3, 2).slice(all)).isEmpty();
    }

    @Test
    void metaCountsPages() {
        assertThat(PageMeta.of(0, 20, 93)).isEqualTo(new PageMeta(0, 20, 5, 93));
        assertThat(PageMeta.of(0, 20, 0)).isEqualTo(new PageMeta(0, 20, 0, 0));
        assertThat(PageMeta.of(1, 10, 10)).isEqualTo(new PageMeta(1, 10, 1, 10));
    }

    @Test
    void decimalsFormatting() {
        assertThat(Decimals.price(new java.math.BigDecimal("450000.0000")).toPlainString()).isEqualTo("450000.00");
        assertThat(Decimals.price(new java.math.BigDecimal("1.2345")).toPlainString()).isEqualTo("1.2345");
        assertThat(Decimals.compact(new java.math.BigDecimal("2.50")).toPlainString()).isEqualTo("2.5");
        assertThat(Decimals.compact(new java.math.BigDecimal("10.00")).toPlainString()).isEqualTo("10");
    }
}
