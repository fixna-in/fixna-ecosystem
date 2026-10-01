package in.fixna.platform.consulting.invoice;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum InvoiceStatus {
    DRAFT,
    SENT,
    PARTIALLY_PAID,
    PAID,
    CANCELLED;

    public void validateTransitionTo(InvoiceStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case DRAFT -> target == SENT || target == CANCELLED;
            case SENT -> target == PARTIALLY_PAID || target == PAID || target == CANCELLED;
            case PARTIALLY_PAID -> target == PAID || target == CANCELLED;
            default -> false;
        };
        if (!allowed) {
            throw invalidTransition(target);
        }
    }

    public boolean isTerminal() {
        return this == PAID || this == CANCELLED;
    }

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean acceptsPayment() {
        return this == SENT || this == PARTIALLY_PAID;
    }

    private FixnaException invalidTransition(InvoiceStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition invoice from " + this + " to " + target);
    }
}
