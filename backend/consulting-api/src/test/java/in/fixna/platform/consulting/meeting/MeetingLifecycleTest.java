package in.fixna.platform.consulting.meeting;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingLifecycleTest {

    @Test
    void scheduledAllowsStartAndCancel() {
        assertThatCode(() -> MeetingStatus.SCHEDULED.validateTransitionTo(MeetingStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> MeetingStatus.SCHEDULED.validateTransitionTo(MeetingStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> MeetingStatus.SCHEDULED.validateTransitionTo(MeetingStatus.COMPLETED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void inProgressAllowsCompleteAndCancel() {
        assertThatCode(() -> MeetingStatus.IN_PROGRESS.validateTransitionTo(MeetingStatus.COMPLETED))
                .doesNotThrowAnyException();
        assertThatCode(() -> MeetingStatus.IN_PROGRESS.validateTransitionTo(MeetingStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> MeetingStatus.IN_PROGRESS.validateTransitionTo(MeetingStatus.SCHEDULED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void terminalStatesRejectTransitions() {
        assertThatThrownBy(() -> MeetingStatus.COMPLETED.validateTransitionTo(MeetingStatus.SCHEDULED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> MeetingStatus.CANCELLED.validateTransitionTo(MeetingStatus.IN_PROGRESS))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }
}
