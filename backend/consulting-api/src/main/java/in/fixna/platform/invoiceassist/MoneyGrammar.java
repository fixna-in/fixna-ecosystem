package in.fixna.platform.invoiceassist;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * Exact decimal money grammar (ADR-012). Parses amounts from invoice text
 * into {@link BigDecimal} with scale ≤ 2 — never a float. Accepts a fixed
 * set of human formats; rejects anything imprecise so it can be flagged for
 * human review rather than silently coerced.
 *
 * Accepted: {@code 1234.56}, {@code 1,234.56}, {@code 1234}, {@code 0.99},
 * with optional surrounding whitespace and a single leading currency symbol
 * (€, $, £, EUR, USD, GBP). Rejected: exponents, >2 fraction digits, empty,
 * multiple separators in a row, bare punctuation.
 */
final class MoneyGrammar {

    private MoneyGrammar() {}

    static Optional<BigDecimal> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        s = stripCurrency(s);
        if (s.isEmpty()) {
            return Optional.empty();
        }
        // Reject scientific notation outright.
        if (s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
            return Optional.empty();
        }

        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        String integerPart;
        String fractionPart = null;

        if (lastDot >= 0 && lastComma >= 0) {
            // Whichever separator appears last is the decimal point.
            if (lastDot > lastComma) {
                integerPart = s.substring(0, lastDot);
                fractionPart = s.substring(lastDot + 1);
                if (!isThousandsGrouped(integerPart, ',')) {
                    return Optional.empty();
                }
                integerPart = integerPart.replace(",", "");
            } else {
                integerPart = s.substring(0, lastComma);
                fractionPart = s.substring(lastComma + 1);
                if (!isThousandsGrouped(integerPart, '.')) {
                    return Optional.empty();
                }
                integerPart = integerPart.replace(".", "");
            }
        } else if (lastComma >= 0) {
            String after = s.substring(lastComma + 1);
            if (after.length() == 3 && isAllDigits(after) && isThousandsGrouped(s.substring(0, lastComma), ',')) {
                // 1,234 -> thousands separator, no fraction
                integerPart = s.replace(",", "");
            } else if (after.length() <= 2 && isAllDigits(after)) {
                integerPart = s.substring(0, lastComma);
                fractionPart = after;
                if (!integerPart.replace(",", "").chars().allMatch(Character::isDigit)) {
                    return Optional.empty();
                }
                integerPart = integerPart.replace(",", "");
            } else {
                return Optional.empty();
            }
        } else if (lastDot >= 0) {
            String after = s.substring(lastDot + 1);
            if (after.length() <= 2 && isAllDigits(after)) {
                integerPart = s.substring(0, lastDot);
                fractionPart = after;
            } else {
                return Optional.empty();
            }
        } else {
            integerPart = s;
        }

        integerPart = integerPart.replace(",", "");
        if (!isAllDigits(integerPart) || integerPart.isEmpty()) {
            return Optional.empty();
        }
        if (fractionPart != null && !isAllDigits(fractionPart)) {
            return Optional.empty();
        }
        if (fractionPart != null && fractionPart.length() > 2) {
            return Optional.empty();
        }

        String canonical = fractionPart == null || fractionPart.isEmpty()
                ? integerPart
                : integerPart + "." + fractionPart;
        try {
            return Optional.of(new BigDecimal(canonical).setScale(2, java.math.RoundingMode.UNNECESSARY));
        } catch (ArithmeticException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String stripCurrency(String s) {
        String upper = s.toUpperCase(Locale.ROOT);
        for (String token : new String[] {"EUR", "USD", "GBP"}) {
            if (upper.startsWith(token)) {
                s = s.substring(token.length()).trim();
                break;
            }
            if (upper.endsWith(token)) {
                s = s.substring(0, s.length() - token.length()).trim();
                break;
            }
        }
        if (!s.isEmpty()) {
            char first = s.charAt(0);
            if (first == '€' || first == '$' || first == '£') {
                s = s.substring(1).trim();
            }
        }
        return s;
    }

    private static boolean isThousandsGrouped(String integerPart, char separator) {
        if (integerPart.isEmpty()) {
            return false;
        }
        String[] groups = integerPart.split(java.util.regex.Pattern.quote(String.valueOf(separator)), -1);
        if (groups.length == 1) {
            return true; // no separator present is fine
        }
        if (!isAllDigits(groups[0]) || groups[0].length() < 1 || groups[0].length() > 3) {
            return false;
        }
        for (int i = 1; i < groups.length; i++) {
            if (!isAllDigits(groups[i]) || groups[i].length() != 3) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllDigits(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}