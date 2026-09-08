package pl.servicedesk.common.error;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "duplicate-resource", message);
    }
}
