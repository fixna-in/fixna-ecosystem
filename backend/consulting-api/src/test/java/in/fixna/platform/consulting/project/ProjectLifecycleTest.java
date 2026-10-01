package in.fixna.platform.consulting.project;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.engagement.EngagementStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectLifecycleTest {

    @Test
    void engagementAllowsDraftToActiveAndCancel() {
        assertThatCode(() -> EngagementStatus.DRAFT.validateTransitionTo(EngagementStatus.ACTIVE))
                .doesNotThrowAnyException();
        assertThatCode(() -> EngagementStatus.DRAFT.validateTransitionTo(EngagementStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> EngagementStatus.DRAFT.validateTransitionTo(EngagementStatus.COMPLETED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void engagementAllowsActiveOnHoldCycle() {
        assertThatCode(() -> EngagementStatus.ACTIVE.validateTransitionTo(EngagementStatus.ON_HOLD))
                .doesNotThrowAnyException();
        assertThatCode(() -> EngagementStatus.ON_HOLD.validateTransitionTo(EngagementStatus.ACTIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void engagementTerminalStatesRejectTransitions() {
        assertThatThrownBy(() -> EngagementStatus.COMPLETED.validateTransitionTo(EngagementStatus.ACTIVE))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void projectAllowsPlannedToInProgress() {
        assertThatCode(() -> ProjectStatus.PLANNED.validateTransitionTo(ProjectStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ProjectStatus.PLANNED.validateTransitionTo(ProjectStatus.COMPLETED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void projectAllowsInProgressOnHoldCycle() {
        assertThatCode(() -> ProjectStatus.IN_PROGRESS.validateTransitionTo(ProjectStatus.ON_HOLD))
                .doesNotThrowAnyException();
        assertThatCode(() -> ProjectStatus.ON_HOLD.validateTransitionTo(ProjectStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
    }

    @Test
    void milestoneFollowsPendingInProgressCompletedPath() {
        assertThatCode(() -> MilestoneStatus.PENDING.validateTransitionTo(MilestoneStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> MilestoneStatus.IN_PROGRESS.validateTransitionTo(MilestoneStatus.COMPLETED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> MilestoneStatus.PENDING.validateTransitionTo(MilestoneStatus.COMPLETED))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void taskAllowsTodoInProgressBlockedDonePath() {
        assertThatCode(() -> TaskStatus.TODO.validateTransitionTo(TaskStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskStatus.IN_PROGRESS.validateTransitionTo(TaskStatus.BLOCKED))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskStatus.BLOCKED.validateTransitionTo(TaskStatus.DONE))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskStatus.TODO.validateTransitionTo(TaskStatus.DONE))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }
}
