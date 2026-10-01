package in.fixna.platform.consulting.proposal;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

public enum ProposalStatus {
    DRAFT,
    SENT,
    APPROVED,
    REJECTED,
    CANCELLED;

    public void validateTransitionTo(ProposalStatus target) {
        if (this == target) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition(target);
        }
        boolean allowed = switch (this) {
            case DRAFT -> target == SENT || target == CANCELLED;
            case SENT -> target == APPROVED || target == REJECTED || target == CANCELLED;
            default -> false;
        };
        if (!allowed) {
            throw invalidTransition(target);
        }
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }

    public boolean isEditable() {
        return this == DRAFT;
    }

    private FixnaException invalidTransition(ProposalStatus target) {
        return new FixnaException(
                "INVALID_STATUS_TRANSITION",
                HttpStatus.BAD_REQUEST,
                "Cannot transition proposal from " + this + " to " + target);
    }
}
