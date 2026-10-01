package in.fixna.platform.consulting.project;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum MilestoneStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public void validateTransitionTo(MilestoneStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case PENDING -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            default -> false;
        };
        if (!allowed) {
            throw invalidTransition(target);
        }
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    private FixnaException invalidTransition(MilestoneStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition milestone from " + this + " to " + target);
    }
}
