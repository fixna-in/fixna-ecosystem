package in.fixna.platform.consulting.meeting;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum MeetingStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public void validateTransitionTo(MeetingStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case SCHEDULED -> target == IN_PROGRESS || target == CANCELLED;
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

    private FixnaException invalidTransition(MeetingStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition meeting from " + this + " to " + target);
    }
}
