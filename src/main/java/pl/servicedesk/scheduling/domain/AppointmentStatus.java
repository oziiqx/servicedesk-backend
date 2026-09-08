package pl.servicedesk.scheduling.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum AppointmentStatus {

    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(IN_PROGRESS, CANCELLED, NO_SHOW),
            IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(AppointmentStatus.class),
            CANCELLED, EnumSet.noneOf(AppointmentStatus.class),
            NO_SHOW, EnumSet.noneOf(AppointmentStatus.class));

    public boolean canTransitionTo(AppointmentStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }

    public boolean blocksSlot() {
        return this != CANCELLED && this != NO_SHOW;
    }

    public static Set<AppointmentStatus> slotBlockingStatuses() {
        return EnumSet.complementOf(EnumSet.of(CANCELLED, NO_SHOW));
    }
}
