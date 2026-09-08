package pl.servicedesk.common.error;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends ApplicationException {

    public BusinessRuleViolationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "business-rule-violation", message);
    }
}
