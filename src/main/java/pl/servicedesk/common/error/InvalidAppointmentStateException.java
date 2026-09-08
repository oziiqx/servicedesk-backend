package pl.servicedesk.common.error;

import org.springframework.http.HttpStatus;

public class InvalidAppointmentStateException extends ApplicationException {

    public InvalidAppointmentStateException(String message) {
        super(HttpStatus.CONFLICT, "invalid-appointment-state", message);
    }
}
