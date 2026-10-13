package in.fixna.invoiceassistant.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MoneyGrammarTest {

    @Test
    void parsesUsStyleThousands() {
        assertThat(MoneyGrammar.parse("1,234.56")).contains(new BigDecimal("1234.56"));
    }

    @Test
    void parsesEuStyleThousands() {
        assertThat(MoneyGrammar.parse("1.234,56")).contains(new BigDecimal("1234.56"));
    }

    @Test
    void parsesSingleCommaAsDecimal() {
        assertThat(MoneyGrammar.parse("1234,5")).contains(new BigDecimal("1234.50"));
    }

    @Test
    void stripsCurrencySymbolsAndCodes() {
        assertThat(MoneyGrammar.parse("€12.00")).contains(new BigDecimal("12.00"));
        assertThat(MoneyGrammar.parse("EUR 99")).contains(new BigDecimal("99.00"));
        assertThat(MoneyGrammar.parse("$1,000")).contains(new BigDecimal("1000.00"));
    }

    @Test
    void plainIntegerBecomesScaleTwo() {
        assertThat(MoneyGrammar.parse("12")).contains(new BigDecimal("12.00"));
    }

    @Test
    void rejectsTooManyFractionDigits() {
        assertThat(MoneyGrammar.parse("12.345")).isEmpty();
    }

    @Test
    void rejectsGarbageAndScientificNotation() {
        assertThat(MoneyGrammar.parse("abc")).isEmpty();
        assertThat(MoneyGrammar.parse("1e5")).isEmpty();
        assertThat(MoneyGrammar.parse("")).isEmpty();
        assertThat(MoneyGrammar.parse(null)).isEmpty();
    }

    @Test
    void rejectsLoneDotWithoutFraction() {
        assertThat(MoneyGrammar.parse("12.345.678")).isEmpty();
    }
}
