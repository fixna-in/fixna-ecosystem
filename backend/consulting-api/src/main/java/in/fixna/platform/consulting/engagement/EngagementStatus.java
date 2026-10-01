package in.fixna.platform.consulting.engagement;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum EngagementStatus {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    public void validateTransitionTo(EngagementStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case DRAFT -> target == ACTIVE || target == CANCELLED;
            case ACTIVE -> target == ON_HOLD || target == COMPLETED || target == CANCELLED;
            case ON_HOLD -> target == ACTIVE || target == COMPLETED || target == CANCELLED;
            default -> false;
        };
        if (!allowed) {
            throw invalidTransition(target);
        }
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    private FixnaException invalidTransition(EngagementStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition engagement from " + this + " to " + target);
    }
}
