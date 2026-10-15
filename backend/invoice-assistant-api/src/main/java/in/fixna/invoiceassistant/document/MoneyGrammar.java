package in.fixna.invoiceassistant.document;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

public final class MoneyGrammar {

    private MoneyGrammar() {}

    public static Optional<BigDecimal> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        s = stripCurrency(s);
        if (s.isEmpty() || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
            return Optional.empty();
        }
        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        String integerPart;
        String fractionPart = null;
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastDot > lastComma) {
                integerPart = s.substring(0, lastDot).replace(",", "");
                fractionPart = s.substring(lastDot + 1);
            } else {
                integerPart = s.substring(0, lastComma).replace(".", "");
                fractionPart = s.substring(lastComma + 1);
            }
        } else if (lastComma >= 0) {
            String after = s.substring(lastComma + 1);
            if (after.length() <= 2 && isAllDigits(after)) {
                integerPart = s.substring(0, lastComma).replace(",", "");
                fractionPart = after;
            } else {
                integerPart = s.replace(",", "");
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
        if (fractionPart != null && (!isAllDigits(fractionPart) || fractionPart.length() > 2)) {
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
        for (String token : new String[] {"EUR", "USD", "GBP", "INR"}) {
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
            if (first == '€' || first == '$' || first == '£' || first == '₹') {
                s = s.substring(1).trim();
            }
        }
        return s;
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
