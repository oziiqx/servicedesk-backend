package pl.servicedesk.common.error;

import org.springframework.http.HttpStatus;

public class SlotUnavailableException extends ApplicationException {

    public SlotUnavailableException(String message) {
        super(HttpStatus.CONFLICT, "slot-unavailable", message);
    }
}
