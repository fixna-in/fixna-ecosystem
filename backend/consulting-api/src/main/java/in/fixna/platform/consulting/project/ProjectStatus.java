package in.fixna.platform.consulting.project;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum ProjectStatus {
    PLANNED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    public void validateTransitionTo(ProjectStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case PLANNED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == ON_HOLD || target == COMPLETED || target == CANCELLED;
            case ON_HOLD -> target == IN_PROGRESS || target == COMPLETED || target == CANCELLED;
            default -> false;
        };
        if (!allowed) {
            throw invalidTransition(target);
        }
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    private FixnaException invalidTransition(ProjectStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition project from " + this + " to " + target);
    }
}
