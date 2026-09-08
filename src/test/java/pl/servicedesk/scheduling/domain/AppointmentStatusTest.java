package pl.servicedesk.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppointmentStatusTest {

    @Test
    void allowsTheHappyPathThroughTheLifecycle() {
        assertThat(AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.CONFIRMED)).isTrue();
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.IN_PROGRESS)).isTrue();
        assertThat(AppointmentStatus.IN_PROGRESS.canTransitionTo(AppointmentStatus.COMPLETED)).isTrue();
    }

    @Test
    void rejectsSkippingStates() {
        assertThat(AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.IN_PROGRESS)).isFalse();
        assertThat(AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.COMPLETED)).isFalse();
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.COMPLETED)).isFalse();
    }

    @Test
    void treatsCompletedCancelledAndNoShowAsTerminal() {
        assertThat(AppointmentStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(AppointmentStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(AppointmentStatus.NO_SHOW.isTerminal()).isTrue();
        assertThat(AppointmentStatus.COMPLETED.canTransitionTo(AppointmentStatus.CANCELLED)).isFalse();
    }

    @Test
    void reportsWhichStatusesStillBlockASlot() {
        assertThat(AppointmentStatus.slotBlockingStatuses())
                .containsExactlyInAnyOrder(
                        AppointmentStatus.PENDING,
                        AppointmentStatus.CONFIRMED,
                        AppointmentStatus.IN_PROGRESS,
                        AppointmentStatus.COMPLETED);
    }
}
