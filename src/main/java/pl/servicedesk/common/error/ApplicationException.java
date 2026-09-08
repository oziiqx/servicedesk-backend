package pl.servicedesk.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final String problemType;

    protected ApplicationException(HttpStatus status, String problemType, String message) {
        super(message);
        this.status = status;
        this.problemType = problemType;
    }
}
