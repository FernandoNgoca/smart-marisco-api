package mz.com.sgp.validation;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import mz.com.sgp.exception.InvalidQuantityException;

class QuantityRulesTests {
    @Test void acceptsDatabaseBoundaryAndFractionalUnits() {
        assertThatCode(() -> QuantityRules.positive(new BigDecimal("9999999.999"))).doesNotThrowAnyException();
        assertThatCode(() -> QuantityRules.positive(new BigDecimal("0.001"))).doesNotThrowAnyException();
        assertThatCode(() -> QuantityRules.positive(new BigDecimal("1.23000"))).doesNotThrowAnyException();
        assertThatCode(() -> QuantityRules.stock(BigDecimal.ZERO)).doesNotThrowAnyException();
    }
    @Test void rejectsOverflowAndTruncationBeforePersistence() {
        for (String value : new String[]{"10000000", "-1", "0.0001", "9999999.9999", "1E100"}) {
            assertThatThrownBy(() -> QuantityRules.positive(new BigDecimal(value))).isInstanceOf(InvalidQuantityException.class);
        }
        assertThatThrownBy(() -> QuantityRules.positive(null)).isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> QuantityRules.positive(BigDecimal.ZERO)).isInstanceOf(InvalidQuantityException.class);
    }
    @Test void validatesFinalBalanceEvenWhenEachInputFits() {
        assertThatThrownBy(() -> QuantityRules.balance(new BigDecimal("9999999.999"), new BigDecimal("0.001"), true))
                .isInstanceOf(InvalidQuantityException.class).hasMessageContaining("Quantidade máxima a entrar");
        assertThat(QuantityRules.balance(new BigDecimal("9999999.998"), new BigDecimal("0.001"), true))
                .isEqualByComparingTo(QuantityRules.MAX);
        assertThatThrownBy(() -> QuantityRules.balance(BigDecimal.ONE, BigDecimal.TEN, false))
                .isInstanceOf(InvalidQuantityException.class).hasMessageContaining("Stock insuficiente");
        assertThat(QuantityRules.balance(BigDecimal.ONE, BigDecimal.ONE, false)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
