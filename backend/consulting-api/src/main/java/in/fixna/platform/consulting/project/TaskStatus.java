package in.fixna.platform.consulting.project;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    BLOCKED,
    DONE,
    CANCELLED;

    public void validateTransitionTo(TaskStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case TODO -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == BLOCKED || target == DONE || target == CANCELLED;
            case BLOCKED -> target == IN_PROGRESS || target == DONE || target == CANCELLED;
            default -> false;
        };
        if (!allowed) {
            throw invalidTransition(target);
        }
    }

    public boolean isTerminal() {
        return this == DONE || this == CANCELLED;
    }

    private FixnaException invalidTransition(TaskStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition task from " + this + " to " + target);
    }
}
