package mz.com.sgp.validation;

import java.math.BigDecimal;
import mz.com.sgp.exception.InvalidQuantityException;

/** Limites das colunas QUANTITY DECIMAL(10,3) nas migrações V5 e V6. */
public final class QuantityRules {
    public static final BigDecimal MAX = new BigDecimal("9999999.999");
    private QuantityRules() {}

    public static void stock(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX) > 0
                || value.stripTrailingZeros().scale() > 3) {
            throw new InvalidQuantityException(
                    "A quantidade deve estar entre 0 e 9 999 999,999, com no máximo 3 casas decimais.");
        }
    }

    public static void positive(BigDecimal value) {
        stock(value);
        if (value.signum() == 0) throw new InvalidQuantityException("A quantidade deve ser maior que zero.");
    }

    public static BigDecimal balance(BigDecimal current, BigDecimal movement, boolean entry) {
        stock(current);
        positive(movement);
        BigDecimal result = entry ? current.add(movement) : current.subtract(movement);
        if (result.compareTo(MAX) > 0) {
            throw new InvalidQuantityException("A entrada excede o limite do stock (9 999 999,999). Quantidade máxima a entrar: "
                    + MAX.subtract(current).toPlainString() + ".");
        }
        if (result.signum() < 0) throw new InvalidQuantityException("Stock insuficiente para esta saída.");
        return result;
    }
}
