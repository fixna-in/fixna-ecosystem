package in.fixna.platform.consulting.proposal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProposalLifecycleTest {

    @Test
    void draftAllowsSendAndCancel() {
        assertThatCode(() -> ProposalStatus.DRAFT.validateTransitionTo(ProposalStatus.SENT))
                .doesNotThrowAnyException();
        assertThatCode(() -> ProposalStatus.DRAFT.validateTransitionTo(ProposalStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ProposalStatus.DRAFT.validateTransitionTo(ProposalStatus.APPROVED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void sentAllowsApproveRejectAndCancel() {
        assertThatCode(() -> ProposalStatus.SENT.validateTransitionTo(ProposalStatus.APPROVED))
                .doesNotThrowAnyException();
        assertThatCode(() -> ProposalStatus.SENT.validateTransitionTo(ProposalStatus.REJECTED))
                .doesNotThrowAnyException();
        assertThatCode(() -> ProposalStatus.SENT.validateTransitionTo(ProposalStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ProposalStatus.SENT.validateTransitionTo(ProposalStatus.DRAFT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void terminalStatesRejectTransitions() {
        assertThatThrownBy(() -> ProposalStatus.APPROVED.validateTransitionTo(ProposalStatus.SENT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> ProposalStatus.REJECTED.validateTransitionTo(ProposalStatus.SENT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
        assertThatThrownBy(() -> ProposalStatus.CANCELLED.validateTransitionTo(ProposalStatus.DRAFT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }
}
